package com.campus.network

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.campus.network.core.ui.theme.CampusNetworkTheme
import com.campus.network.ui.MainAppScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as CampusApplication

        setContent {
            CampusNetworkTheme {
                MainAppScreen(
                    database = app.database,
                    streamChatEngine = app.streamChatEngine
                )
            }
        }
    }
}
