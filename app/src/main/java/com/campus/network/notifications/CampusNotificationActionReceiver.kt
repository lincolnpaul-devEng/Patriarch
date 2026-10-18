package com.campus.network.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.campus.network.core.ui.notifications.CampusSystemNotificationCenter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * BroadcastReceiver handling interactive actions directly from the Android Notification Shade:
 * 1. WhatsApp-style inline `RemoteInput` text replies (`ACTION_INLINE_REPLY`)
 * 2. Mark conversation as read (`ACTION_MARK_AS_READ`)
 * 3. Decline incoming audio/video call (`ACTION_DECLINE_CALL`)
 * 4. Check in to Map Flash-Meet (`ACTION_CHECK_IN_MEET`)
 *
 * Uses `goAsync()` on `Dispatchers.Default` so notification rebuilding and bitmap/state
 * updates never block the Android Main Thread or risk BroadcastReceiver ANRs.
 */
class CampusNotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val appContext = context.applicationContext
        val notificationId = intent.getIntExtra(CampusSystemNotificationCenter.EXTRA_NOTIFICATION_ID, 0)
        val remoteInputBundle = RemoteInput.getResultsFromIntent(intent)
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
            try {
                when (action) {
                    CampusSystemNotificationCenter.ACTION_INLINE_REPLY -> {
                        val replyText = remoteInputBundle
                            ?.getCharSequence(CampusSystemNotificationCenter.KEY_TEXT_REPLY)
                            ?.toString()
                            ?.trim()
                            .orEmpty()

                        if (replyText.isNotEmpty()) {
                            val threadId = intent.getStringExtra(CampusSystemNotificationCenter.EXTRA_THREAD_ID) ?: "thread_mwangi"
                            val peerName = intent.getStringExtra(CampusSystemNotificationCenter.EXTRA_PEER_NAME) ?: "Peer"
                            val peerHandle = intent.getStringExtra(CampusSystemNotificationCenter.EXTRA_PEER_HANDLE) ?: ""

                            CampusSystemNotificationCenter.onInlineReplySubmitted(
                                context = appContext,
                                notificationId = notificationId,
                                threadId = threadId,
                                peerName = peerName,
                                peerHandle = peerHandle,
                                replyText = replyText
                            )
                        }
                    }

                    CampusSystemNotificationCenter.ACTION_MARK_AS_READ -> {
                        val threadId = intent.getStringExtra(CampusSystemNotificationCenter.EXTRA_THREAD_ID) ?: return@launch
                        CampusSystemNotificationCenter.onMarkThreadAsRead(
                            context = appContext,
                            notificationId = notificationId,
                            threadId = threadId
                        )
                    }

                    CampusSystemNotificationCenter.ACTION_DECLINE_CALL -> {
                        if (notificationId != 0) {
                            NotificationManagerCompat.from(appContext).cancel(notificationId)
                        }
                    }

                    CampusSystemNotificationCenter.ACTION_CHECK_IN_MEET -> {
                        val meetTitle = intent.getStringExtra(CampusSystemNotificationCenter.EXTRA_MEET_TITLE) ?: "Flash-Meet"
                        if (notificationId != 0) {
                            NotificationManagerCompat.from(appContext).cancel(notificationId)
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(appContext, "Checked in to $meetTitle", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
