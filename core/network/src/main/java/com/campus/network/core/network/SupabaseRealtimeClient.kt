package com.campus.network.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.net.URLEncoder
import java.util.concurrent.atomic.AtomicInteger

enum class RealtimeEventType {
    INSERT,
    UPDATE,
    DELETE
}

data class SupabaseRealtimeChange(
    val table: String,
    val eventType: RealtimeEventType,
    val recordJson: String? = null,
    val oldRecordJson: String? = null
)

private val realtimeJsonParser = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

internal fun buildSupabaseRealtimeWebSocketUrl(
    supabaseUrl: String,
    anonKey: String
): String {
    val trimmed = supabaseUrl.trim().trimEnd('/')
    val wsBase = when {
        trimmed.startsWith("https://", ignoreCase = true) -> "wss://" + trimmed.substring(8)
        trimmed.startsWith("http://", ignoreCase = true) -> "ws://" + trimmed.substring(7)
        trimmed.isNotBlank() -> "wss://$trimmed"
        else -> return ""
    }
    val encodedKey = URLEncoder.encode(anonKey.trim(), "UTF-8")
    return "$wsBase/realtime/v1/websocket?apikey=$encodedKey&vsn=1.0.0"
}

internal fun buildPostgresChangesJoinFrame(
    table: String,
    accessToken: String,
    ref: String
): String {
    val frame = buildJsonObject {
        put("topic", "realtime:public:$table")
        put("event", "phx_join")
        put(
            "payload",
            buildJsonObject {
                put(
                    "config",
                    buildJsonObject {
                        put("broadcast", buildJsonObject { put("self", false) })
                        put("presence", buildJsonObject { put("key", "") })
                        put(
                            "postgres_changes",
                            buildJsonArray {
                                add(
                                    buildJsonObject {
                                        put("event", "*")
                                        put("schema", "public")
                                        put("table", table)
                                    }
                                )
                            }
                        )
                    }
                )
                put("access_token", accessToken)
            }
        )
        put("ref", ref)
    }
    return frame.toString()
}

internal fun buildHeartbeatFrame(ref: String): String =
    buildJsonObject {
        put("topic", "phoenix")
        put("event", "heartbeat")
        put("payload", JsonObject(emptyMap()))
        put("ref", ref)
    }.toString()

internal fun buildAccessTokenFrame(topic: String, accessToken: String, ref: String): String =
    buildJsonObject {
        put("topic", topic)
        put("event", "access_token")
        put("payload", buildJsonObject { put("access_token", accessToken) })
        put("ref", ref)
    }.toString()

internal fun parseSupabaseRealtimeFrame(rawJson: String): SupabaseRealtimeChange? {
    val root = runCatching { realtimeJsonParser.parseToJsonElement(rawJson) as? JsonObject }.getOrNull()
        ?: return null
    val event = (root["event"] as? JsonPrimitive)?.contentOrNull ?: return null
    if (!event.equals("postgres_changes", ignoreCase = true)) {
        return null
    }
    val payload = root["payload"] as? JsonObject ?: return null
    val data = payload["data"] as? JsonObject

    val rawType = ((data?.get("type") ?: payload["eventType"]) as? JsonPrimitive)?.contentOrNull?.uppercase()
        ?: return null
    val eventType = when (rawType) {
        "INSERT" -> RealtimeEventType.INSERT
        "UPDATE" -> RealtimeEventType.UPDATE
        "DELETE" -> RealtimeEventType.DELETE
        else -> return null
    }

    val topicTable = (root["topic"] as? JsonPrimitive)?.contentOrNull
        ?.substringAfterLast(":")
        ?.takeIf { it.isNotBlank() && it != "realtime" }
    val table = ((data?.get("table") ?: payload["table"]) as? JsonPrimitive)?.contentOrNull
        ?.takeIf { it.isNotBlank() }
        ?: topicTable
        ?: return null

    val recordObj = (data?.get("record") ?: payload["record"] ?: payload["new"]) as? JsonObject
    val oldRecordObj = (data?.get("old_record") ?: payload["old_record"] ?: payload["old"]) as? JsonObject

    return SupabaseRealtimeChange(
        table = table,
        eventType = eventType,
        recordJson = recordObj?.toString(),
        oldRecordJson = oldRecordObj?.toString()
    )
}

/**
 * Phoenix-protocol WebSocket client for Supabase Realtime Postgres Changes (`postgres_changes`).
 * Automatically authenticates with the current Supabase Auth JWT and reconnects with backoff.
 */
class SupabaseRealtimeClient(
    private val supabaseUrl: String = BuildConfig.SUPABASE_URL,
    private val supabaseKey: String = BuildConfig.SUPABASE_ANON_KEY,
    private val accessTokenProvider: suspend () -> String? = {
        runCatching {
            SupabaseSessionStore.validAccessToken(
                supabaseUrl = supabaseUrl,
                anonKey = supabaseKey
            )
        }.getOrNull()
    },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val scope = CoroutineScope(ioDispatcher + SupervisorJob())
    private val httpClient by lazy {
        HttpClient(OkHttp) {
            install(WebSockets) {
                pingInterval = 20_000L
            }
        }
    }

    private val _changes = MutableSharedFlow<SupabaseRealtimeChange>(extraBufferCapacity = 128)
    val changes: SharedFlow<SupabaseRealtimeChange> = _changes.asSharedFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val refCounter = AtomicInteger(1)
    @Volatile
    private var subscribedTables: List<String> = emptyList()
    @Volatile
    private var connectionJob: Job? = null

    fun start(tables: List<String>) {
        val cleanTables = tables.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        if (cleanTables.isEmpty() || supabaseUrl.isBlank() || supabaseKey.isBlank()) return
        if (connectionJob?.isActive == true && subscribedTables == cleanTables) return

        subscribedTables = cleanTables
        launchConnectionLoop()
    }

    fun reconnect() {
        if (subscribedTables.isEmpty()) return
        launchConnectionLoop()
    }

    fun stop() {
        connectionJob?.cancel()
        connectionJob = null
        _isConnected.value = false
    }

    private fun launchConnectionLoop() {
        connectionJob?.cancel()
        connectionJob = scope.launch {
            val wsUrl = buildSupabaseRealtimeWebSocketUrl(supabaseUrl, supabaseKey)
            if (wsUrl.isBlank()) return@launch

            var backoffMs = INITIAL_BACKOFF_MS
            while (isActive) {
                var heartbeatJob: Job? = null
                try {
                    val token = accessTokenProvider()?.takeIf { it.isNotBlank() } ?: supabaseKey
                    httpClient.webSocket(urlString = wsUrl) {
                        subscribedTables.forEach { table ->
                            val joinFrame = buildPostgresChangesJoinFrame(
                                table = table,
                                accessToken = token,
                                ref = refCounter.getAndIncrement().toString()
                            )
                            send(Frame.Text(joinFrame))
                        }
                        _isConnected.value = true
                        backoffMs = INITIAL_BACKOFF_MS

                        heartbeatJob = launch {
                            while (isActive) {
                                delay(HEARTBEAT_INTERVAL_MS)
                                runCatching {
                                    send(Frame.Text(buildHeartbeatFrame(refCounter.getAndIncrement().toString())))
                                    val freshToken = accessTokenProvider()?.takeIf { it.isNotBlank() }
                                    if (freshToken != null) {
                                        subscribedTables.forEach { table ->
                                            send(
                                                Frame.Text(
                                                    buildAccessTokenFrame(
                                                        topic = "realtime:public:$table",
                                                        accessToken = freshToken,
                                                        ref = refCounter.getAndIncrement().toString()
                                                    )
                                                )
                                            )
                                        }
                                    }
                                }.onFailure {
                                    if (it is CancellationException) throw it
                                }
                            }
                        }

                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                parseSupabaseRealtimeFrame(frame.readText())?.let { change ->
                                    _changes.emit(change)
                                }
                            }
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Throwable) {
                    // Connection dropped or handshake failed; reconnect after backoff.
                } finally {
                    heartbeatJob?.cancel()
                    _isConnected.value = false
                }

                delay(backoffMs)
                backoffMs = (backoffMs * 2L).coerceAtMost(MAX_BACKOFF_MS)
            }
        }
    }

    private companion object {
        const val HEARTBEAT_INTERVAL_MS = 25_000L
        const val INITIAL_BACKOFF_MS = 2_000L
        const val MAX_BACKOFF_MS = 30_000L
    }
}
