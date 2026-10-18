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
        handleTriggerNotificationsIntent(intent)

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
        handleTriggerNotificationsIntent(intent)
    }

    private fun handleTriggerNotificationsIntent(intent: Intent?) {
        if (BuildConfig.DEBUG && intent?.getBooleanExtra("trigger_notifications_now", false) == true) {
            CampusSystemNotificationCenter.setPushEnabled(this, true)
            CampusSystemNotificationCenter.showDirectMessageNotification(
                context = this,
                threadId = "thread_mwangi",
                senderName = "Prof. Mwangi (Supervisor)",
                senderHandle = "@prof_mwangi",
                messageText = "Please review chapter 4 citations before tomorrow's seminar. Tap Reply below to respond directly from your notification shade!",
                avatarColorArgb = 0xFF014784.toInt()
            )
            CampusSystemNotificationCenter.showFlashMeetNotification(
                context = this,
                meetId = "meet_cs_sprint",
                title = "CS Finals Study Sprint",
                locationName = "Central Quad Pod #4",
                attendeeCount = 14,
                timeRemainingLabel = "Starting in 5 min",
                latitude = -1.3094,
                longitude = 36.8122
            )
            CampusSystemNotificationCenter.showSafePayOrderNotification(
                context = this,
                orderRef = "ORD-8492",
                itemTitle = "TI-84 Plus CE Graphing Calculator",
                amountFormatted = "KES 2,500",
                pickupSpotName = "North Quad SafeZone",
                sellerName = "Brian Ochieng",
                statusHeadline = "SafePay Escrow Locked • Ready for QR Scan"
            )
        }
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
