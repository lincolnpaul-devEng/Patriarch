package com.campus.network

import android.app.Application
import android.os.Build
import androidx.emoji2.bundled.BundledEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.campus.network.core.database.CampusDatabase
import com.campus.network.core.network.StreamChatEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CampusApplication : Application(), ImageLoaderFactory {

    private val applicationScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /** True on rooted/emulated/sideloaded devices; escrow release and live location should be restricted. */
    var isElevatedRiskDevice: Boolean = false
        private set

    lateinit var database: CampusDatabase
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

        // 2. Initialize Room Local Cache
        database = CampusDatabase.getInstance(this)

        // 3. Start Hybrid Polling + FCM System Notification Engine
        com.campus.network.notifications.CampusNotificationPollManager.start(this)

        // 4. Offload Stream Chat engine initialization to secondary worker thread
        applicationScope.launch {
            runCatching {
                val engine = StreamChatEngine(this@CampusApplication)
                // Real Stream API Key from UniHub
                engine.initialize(apiKey = BuildConfig.STREAM_API_KEY)
                streamChatEngine = engine
            }
        }
    }
}
