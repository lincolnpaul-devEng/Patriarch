package com.campus.network

import android.app.Application
import com.campus.network.core.database.CampusDatabase
import com.campus.network.core.network.StreamChatEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CampusApplication : Application() {

    private val applicationScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    lateinit var database: CampusDatabase
        private set

    var streamChatEngine: StreamChatEngine? = null
        private set

    override fun onCreate() {
        super.onCreate()

        // 1. Initialize Room Local Cache
        database = CampusDatabase.getInstance(this)

        // 2. Offload Stream Chat engine initialization to secondary worker thread
        applicationScope.launch {
            runCatching {
                val engine = StreamChatEngine(this@CampusApplication)
                // Real Stream API Key from UniHub
                engine.initialize(apiKey = "ere637qj6ze8")
                streamChatEngine = engine
            }
        }
    }
}
