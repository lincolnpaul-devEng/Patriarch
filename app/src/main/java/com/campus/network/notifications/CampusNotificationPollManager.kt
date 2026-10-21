package com.campus.network.notifications

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.campus.network.BuildConfig
import com.campus.network.core.ui.notifications.CampusSystemNotificationCenter
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Hybrid Polling + FCM Notification Manager.
 *
 * Engineering Best Practices:
 * 1. Lifecycle-aware foreground polling loop (active only while the app is in the foreground)
 *    paired with a Doze-safe `PeriodicWorkRequest` (`CampusNotificationSyncWorker`) via `WorkManager`
 *    when the app moves to the background.
 * 2. Connectivity gate (`NET_CAPABILITY_INTERNET` + `NET_CAPABILITY_VALIDATED`) so zero network
 *    or CPU cycles are wasted when the device is offline.
 * 3. Incremental delta querying (`created_at=gt.<watermark>`), HTTP `If-None-Match` ETag support,
 *    and adaptive exponential backoff with jitter (`45s -> 90s -> 180s` when idle, resetting to `45s` on activity).
 */
object CampusNotificationPollManager {

    private const val WORK_NAME = "campus_background_notification_poll_work"
    private const val PREFS_POLL = "campus_notification_poll_state"
    private const val KEY_INITIAL_BATCH_POSTED = "initial_batch_posted_v1"
    private const val KEY_LAST_CYCLE_INDEX = "last_poll_cycle_index"
    private const val KEY_LAST_CREATED_AT = "last_supabase_created_at"
    private const val KEY_LAST_ETAG = "last_supabase_etag"
    private const val KEY_RECIPIENT_USER_ID = "recipient_user_id"
    private const val KEY_RECIPIENT_USER_EMAIL = "recipient_user_email"

    private const val BASE_INTERVAL_MS = 45_000L
    private const val MAX_BACKOFF_INTERVAL_MS = 180_000L

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var foregroundPollJob: Job? = null
    @Volatile
    private var currentPollIntervalMs: Long = BASE_INTERVAL_MS

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    fun start(context: Context) {
        val appContext = context.applicationContext
        CampusSystemNotificationCenter.ensureChannelsCreated(appContext)

        // 1. Fetch & persist Firebase Cloud Messaging token if Google Play Services is available
        runCatching {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                if (!token.isNullOrBlank()) {
                    CampusSystemNotificationCenter.saveFcmToken(appContext, token)
                }
            }
        }

        // 2. Schedule Periodic Background Polling Worker via WorkManager
        scheduleBackgroundWork(appContext)

        // 3. Start Adaptive Foreground Polling Loop
        resumeForegroundPolling(appContext, isInitialStart = true)
    }

    /**
     * Resumes the fast foreground polling coroutine when the user brings the app to the foreground.
     */
    fun resumeForegroundPolling(context: Context, isInitialStart: Boolean = false) {
        val appContext = context.applicationContext
        if (foregroundPollJob?.isActive == true) return
        currentPollIntervalMs = BASE_INTERVAL_MS

        foregroundPollJob = scope.launch {
            // Brief startup delay so MainActivity UI & POST_NOTIFICATIONS permission check settle
            delay(1_800L)
            pollOnce(appContext, isStartup = true)

            while (isActive) {
                val jitterMs = Random.nextLong(0L, 3_000L)
                delay(currentPollIntervalMs + jitterMs)
                val hadNewEvents = pollOnce(appContext, isStartup = false)
                currentPollIntervalMs = if (hadNewEvents) {
                    BASE_INTERVAL_MS
                } else {
                    (currentPollIntervalMs * 2L).coerceAtMost(MAX_BACKOFF_INTERVAL_MS)
                }
            }
        }
    }

    /**
     * Pauses the fast foreground polling coroutine when the app moves to the background,
     * handing off background sync exclusively to WorkManager (`CampusNotificationSyncWorker`).
     */
    fun pauseForegroundPolling() {
        foregroundPollJob?.cancel()
        foregroundPollJob = null
    }

    private fun scheduleBackgroundWork(context: Context) {
        runCatching {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicWork = PeriodicWorkRequestBuilder<CampusNotificationSyncWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicWork
            )
        }
    }

    private fun isNetworkValidated(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    suspend fun pollOnce(context: Context, isStartup: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        if (!CampusSystemNotificationCenter.isPushEnabled(context)) return@withContext false
        if (!CampusSystemNotificationCenter.hasNotificationPermission(context)) return@withContext false

        // Poll Supabase REST notifications table when validated network connectivity is available
        if (isNetworkValidated(context)) {
            return@withContext pollSupabaseNotificationsTable(context)
        }

        false
    }

    /**
     * Polls campus notifications on demand from backend REST endpoint.
     */
    fun triggerNextPolledNotification(context: Context) {
        scope.launch {
            pollOnce(context, isStartup = false)
        }
    }

    private fun pollSupabaseNotificationsTable(context: Context): Boolean {
        val supabaseUrl = BuildConfig.SUPABASE_URL
        val supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        if (supabaseUrl.isBlank() || supabaseKey.isBlank()) return false

        return runCatching {
            val prefs = context.getSharedPreferences(PREFS_POLL, Context.MODE_PRIVATE)
            val lastSeenId = prefs.getString("last_supabase_notif_id", null)
            val lastCreatedAt = prefs.getString(KEY_LAST_CREATED_AT, null)
            val lastEtag = prefs.getString(KEY_LAST_ETAG, null)
            val recipientId = CampusSystemNotificationCenter.getActiveUserId(context)
                ?: prefs.getString(KEY_RECIPIENT_USER_ID, null)?.takeIf { it.isNotBlank() }
            val recipientEmail = prefs.getString(KEY_RECIPIENT_USER_EMAIL, null)?.takeIf { it.isNotBlank() }

            val recipientFilter = when {
                !recipientId.isNullOrBlank() && !recipientEmail.isNullOrBlank() && !recipientId.equals(recipientEmail, ignoreCase = true) ->
                    "&recipient_user_id=in.(${URLEncoder.encode(recipientId, "UTF-8")},${URLEncoder.encode(recipientEmail, "UTF-8")})"
                !recipientId.isNullOrBlank() ->
                    "&recipient_user_id=eq.${URLEncoder.encode(recipientId, "UTF-8")}"
                !recipientEmail.isNullOrBlank() ->
                    "&recipient_user_id=eq.${URLEncoder.encode(recipientEmail, "UTF-8")}"
                else -> return false // Do not poll when not logged in
            }

            val deltaFilter = if (!lastCreatedAt.isNullOrBlank()) {
                "&created_at=gt.${URLEncoder.encode(lastCreatedAt, "UTF-8")}"
            } else {
                ""
            }

            val requestBuilder = Request.Builder()
                .url(
                    "$supabaseUrl/rest/v1/campus_notifications?select=*" +
                        "&order=created_at.desc&limit=8$recipientFilter$deltaFilter"
                )
                .addHeader("apikey", supabaseKey)
                .addHeader("Authorization", "Bearer $supabaseKey")
                .get()

            if (!lastEtag.isNullOrBlank()) {
                requestBuilder.addHeader("If-None-Match", lastEtag)
            }

            httpClient.newCall(requestBuilder.build()).execute().use { response ->
                if (response.code == 304 || !response.isSuccessful) return false
                val responseEtag = response.header("ETag")
                val bodyStr = response.body?.string() ?: return false
                val jsonArray = JSONArray(bodyStr)
                if (jsonArray.length() == 0) return false

                var postedAny = false
                for (i in 0 until jsonArray.length()) {
                    val newestObj = jsonArray.getJSONObject(i)
                    val id = newestObj.optString("id")
                    val createdAt = newestObj.optString("created_at")
                    if (id.isBlank() || id == lastSeenId) continue
                    if (i == 0) {
                        prefs.edit().apply {
                            putString("last_supabase_notif_id", id)
                            if (createdAt.isNotBlank()) putString(KEY_LAST_CREATED_AT, createdAt)
                            if (!responseEtag.isNullOrBlank()) putString(KEY_LAST_ETAG, responseEtag)
                            apply()
                        }
                    }
                    val title = newestObj.optString("title", "Campus Alert")
                    val message = newestObj.optString("body", newestObj.optString("description", ""))
                    val type = newestObj.optString("type", "DIRECT_MESSAGE").uppercase()
                    val actorName = newestObj.optString("actor_name", title)

                    when (type) {
                        "FLASH_MEET" -> {
                            CampusSystemNotificationCenter.showFlashMeetNotification(
                                context = context,
                                meetId = id,
                                title = title,
                                locationName = newestObj.optString("location", "Central Quad"),
                                attendeeCount = newestObj.optInt("attendees", 12),
                                timeRemainingLabel = "Now",
                                latitude = newestObj.optDouble("latitude", -1.3094),
                                longitude = newestObj.optDouble("longitude", 36.8122)
                            )
                        }
                        "FRIEND_REQUEST" -> {
                            CampusSystemNotificationCenter.showFriendRequestNotification(
                                context = context,
                                peerUserId = newestObj.optString(
                                    "actor_user_id",
                                    newestObj.optString("thread_id", "unknown_peer")
                                ),
                                peerName = actorName,
                                serverId = newestObj.optString("server_id").ifBlank { null },
                                body = message
                            )
                        }
                        "FRIEND_REQUEST_ACCEPTED" -> {
                            CampusSystemNotificationCenter.showFriendRequestNotification(
                                context = context,
                                peerUserId = newestObj.optString(
                                    "actor_user_id",
                                    newestObj.optString("thread_id", "unknown_peer")
                                ),
                                peerName = actorName,
                                serverId = newestObj.optString("server_id").ifBlank { null },
                                body = message.ifBlank { "$actorName accepted your friend request" }
                            )
                        }
                        else -> {
                            CampusSystemNotificationCenter.showDirectMessageNotification(
                                context = context,
                                threadId = newestObj.optString(
                                    "thread_id",
                                    newestObj.optString("actor_user_id", "thread_unknown")
                                ),
                                senderName = actorName.ifBlank { title },
                                messageText = message.ifBlank { "New campus message" }
                            )
                        }
                    }
                    postedAny = true
                }
                postedAny
            }
        }.getOrDefault(false)
    }

    /** Bind poll filter to the signed-in student so we only surface their inbox rows. */
    fun setRecipientUserId(context: Context, userId: String?, email: String? = null) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_POLL, Context.MODE_PRIVATE)
        val oldUserId = prefs.getString(KEY_RECIPIENT_USER_ID, null)
        val oldEmail = prefs.getString(KEY_RECIPIENT_USER_EMAIL, null)
        val newUserId = userId?.trim().orEmpty()
        val newEmail = email?.trim().orEmpty()
        val editor = prefs.edit()
            .putString(KEY_RECIPIENT_USER_ID, newUserId)
            .putString(KEY_RECIPIENT_USER_EMAIL, newEmail)
        if (oldUserId != newUserId || oldEmail != newEmail) {
            editor.remove("last_supabase_notif_id")
            editor.remove(KEY_LAST_CREATED_AT)
            editor.remove(KEY_LAST_ETAG)
        }
        editor.apply()
    }
}

class CampusNotificationSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return runCatching {
            CampusNotificationPollManager.pollOnce(applicationContext, isStartup = false)
            Result.success()
        }.getOrDefault(Result.retry())
    }
}
