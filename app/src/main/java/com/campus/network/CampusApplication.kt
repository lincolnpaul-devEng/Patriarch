package com.campus.network

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import androidx.emoji2.bundled.BundledEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.campus.network.core.data.DeltaSyncManager
import com.campus.network.core.data.SyncWatermarkStore
import com.campus.network.core.database.CampusDatabase
import com.campus.network.core.network.StreamChatEngine
import com.campus.network.core.network.SupabaseSdkClientProvider
import com.campus.network.core.network.SupabaseSessionStore
import com.campus.network.core.network.call.CallMediaProviderRegistry
import com.campus.network.core.network.call.StreamVideoCallMediaProvider
import com.campus.network.notifications.CampusDeltaSyncWorker
import com.campus.network.notifications.CampusNotificationPollManager
import com.campus.network.notifications.SyncRetryWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class CampusApplication : Application(), ImageLoaderFactory {

    private val applicationScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /** True on rooted/emulated/sideloaded devices; escrow release and live location should be restricted. */
    var isElevatedRiskDevice: Boolean = false
        private set

    lateinit var container: AppContainer
        private set

    var streamChatEngine: StreamChatEngine? = null
        private set

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .respectCacheHeaders(false)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        SupabaseSessionStore.initialize(this)

        // 0. Runtime integrity: debugger, tracer, Frida/Xposed, signature. Release builds stop on critical hits.
        val integrity = com.campus.network.core.security.RuntimeIntegrityGuard.evaluate(
            context = this,
            expectedSignatureSha256 = BuildConfig.EXPECTED_SIGNATURE_SHA256
        )
        isElevatedRiskDevice = integrity.isElevatedRisk
        if (!BuildConfig.DEBUG && integrity.isCritical) {
            android.os.Process.killProcess(android.os.Process.myPid())
            return
        }

        // 1. Initialize Bundled Google Unicode Emoji Font (Supports all ~3,700+ emojis offline)
        EmojiCompat.init(BundledEmojiCompatConfig(this))

        // 2. Initialize AppContainer (owns Room Local Cache + app-scoped repositories)
        container = DefaultAppContainer(this)

        // 3. Initialize sync watermark store + schedule periodic delta sync worker
        SyncWatermarkStore.initialize(this)
        CampusDeltaSyncWorker.schedule(this)

        // 4. Schedule periodic sync retry worker for FAILED items
        SyncRetryWorker.schedule(this)

        // 4. Start Hybrid Polling + FCM System Notification Engine
        CampusNotificationPollManager.start(this)

        // 5. Register connectivity observer: trigger immediate delta sync when back online
        registerConnectivityObserver(this)

        // 6. Offload Stream Chat engine initialization to secondary worker thread
        applicationScope.launch {
            runCatching {
                val engine = StreamChatEngine(this@CampusApplication)
                // Real Stream API Key from UniHub
                engine.initialize(apiKey = BuildConfig.STREAM_API_KEY)
                streamChatEngine = engine
            }
        }

        // 7. Stream Video peer A/V — same API key + stream-token edge function as Chat
        if (BuildConfig.STREAM_API_KEY.isNotBlank()) {
            CallMediaProviderRegistry.install(
                StreamVideoCallMediaProvider(
                    appContext = this,
                    apiKey = BuildConfig.STREAM_API_KEY,
                    tokenFetcher = { userId ->
                        SupabaseSdkClientProvider.fetchStreamToken(userId = userId)
                    }
                )
            )
        }
    }

    private fun registerConnectivityObserver(context: Context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                val caps = cm.getNetworkCapabilities(network) ?: return
                if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                ) {
                    // Device came back online — trigger immediate delta sync of all tables
                    applicationScope.launch {
                        runCatching {
                            DeltaSyncManager.get(context).syncAll()
                        }
                    }
                    SyncRetryWorker.runNow(context)
                }
            }
        }
        cm.registerDefaultNetworkCallback(callback)
    }
}
