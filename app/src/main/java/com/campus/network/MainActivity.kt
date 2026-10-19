package com.campus.network

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.campus.network.core.ui.notifications.CampusSystemNotificationCenter
import com.campus.network.core.ui.theme.CampusNetworkTheme
import com.campus.network.notifications.CampusNotificationPollManager
import com.campus.network.ui.MainAppScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!BuildConfig.DEBUG) {
            window.setFlags(
                android.view.WindowManager.LayoutParams.FLAG_SECURE,
                android.view.WindowManager.LayoutParams.FLAG_SECURE
            )
        }
        enableEdgeToEdge()

        CampusSystemNotificationCenter.ensureChannelsCreated(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        CampusSystemNotificationCenter.emitDeepLinkFromIntent(intent)

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

    override fun onStart() {
        super.onStart()
        CampusNotificationPollManager.resumeForegroundPolling(this)
    }

    override fun onStop() {
        super.onStop()
        CampusNotificationPollManager.pauseForegroundPolling()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        CampusSystemNotificationCenter.emitDeepLinkFromIntent(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            CampusNotificationPollManager.triggerNextPolledNotification(this)
        }
    }
}
