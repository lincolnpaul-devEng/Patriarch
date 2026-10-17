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

        // 1. Initialize Bundled Google Unicode Emoji Font (Supports all ~3,700+ emojis offline)
        EmojiCompat.init(BundledEmojiCompatConfig(this))

        // 2. Initialize Room Local Cache
        database = CampusDatabase.getInstance(this)

        // 2. Offload Stream Chat engine initialization to secondary worker thread
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
