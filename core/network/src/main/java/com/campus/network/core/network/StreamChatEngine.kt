package com.campus.network.core.network

import android.content.Context
import com.campus.network.core.model.PeerTransactionPayload
import com.campus.network.core.model.SettlementStatus
import com.campus.network.core.security.EmailValidationGuard
import io.getstream.chat.android.client.ChatClient
import io.getstream.chat.android.client.events.ConnectedEvent
import io.getstream.chat.android.client.events.NewMessageEvent
import io.getstream.chat.android.models.Channel
import io.getstream.chat.android.models.Message
import io.getstream.chat.android.models.User
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Low-level Stream Chat engine wrapper.
 * Strictly guarantees offloading of network payloads to secondary coroutine worker threads.
 */
class StreamChatEngine(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val engineScope = CoroutineScope(ioDispatcher + SupervisorJob())
    private var client: ChatClient? = null

    private val _incomingMessages = MutableSharedFlow<Message>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<Message> = _incomingMessages.asSharedFlow()

    private val _settlementEvents = MutableSharedFlow<PeerTransactionPayload>(extraBufferCapacity = 32)
    val settlementEvents: SharedFlow<PeerTransactionPayload> = _settlementEvents.asSharedFlow()

    fun initialize(apiKey: String): ChatClient {
        if (client == null) {
            client = ChatClient.Builder(apiKey, context.applicationContext)
                .build()

            listenToEvents()
        }
        return client!!
    }

    fun getClient(): ChatClient {
        return checkNotNull(client) { "StreamChatEngine must be initialized before accessing ChatClient" }
    }

    /**
     * Connects an authenticated student ensuring security boundary verification.
     */
    suspend fun connectStudent(
        studentId: String,
        studentEmail: String,
        displayName: String,
        token: String
    ): Result<User> = withContext(ioDispatcher) {
        // Enforce network edge security gate before authenticating socket
        val validation = EmailValidationGuard.validateUniversityEmail(studentEmail)
        if (validation !is EmailValidationGuard.ValidationResult.Authorized) {
            return@withContext Result.failure(
                SecurityException("Authentication rejected: student email does not match trusted university domain.")
            )
        }

        val user = User(
            id = studentId,
            name = displayName,
            extraData = mapOf(
                "email" to validation.normalizedEmail,
                "institution" to validation.institutionName
            )
        )

        val callResult = suspendCancellableChatCall {
            getClient().connectUser(user, token)
        }
        return@withContext callResult.map { it.user }
    }

    /**
     * Sends an inline peer transaction settlement payload inside a Stream Chat channel.
     */
    suspend fun sendTransactionSettlement(
        channelType: String,
        channelId: String,
        payload: PeerTransactionPayload
    ): Result<Message> = withContext(ioDispatcher) {
        val extraData = mutableMapOf<String, Any>(
            "is_settlement_card" to true,
            "tx_id" to payload.transactionId,
            "tx_sender" to payload.senderName,
            "tx_recipient" to payload.recipientName,
            "tx_amount" to payload.amount,
            "tx_currency" to payload.currency,
            "tx_purpose" to payload.purpose,
            "tx_status" to payload.status.name,
            "tx_ref" to payload.referenceCode,
            "tx_timestamp" to payload.timestampMs
        )

        val message = Message(
            text = "Peer Settlement: ${payload.currency} ${payload.amount} for ${payload.purpose} [Ref: ${payload.referenceCode}]",
            extraData = extraData
        )

        return@withContext suspendCancellableChatCall {
            getClient().sendMessage(channelType, channelId, message)
        }
    }

    /**
     * Sends a direct text message inside a channel.
     */
    suspend fun sendDirectMessage(
        channelId: String,
        text: String
    ): Result<Message> = withContext(ioDispatcher) {
        val message = Message(text = text)
        return@withContext suspendCancellableChatCall {
            getClient().sendMessage("messaging", channelId, message)
        }
    }

    private fun listenToEvents() {
        val currentClient = client ?: return

        currentClient.subscribeFor(
            NewMessageEvent::class.java
        ) { event ->
            if (event is NewMessageEvent) {
                engineScope.launch {
                    _incomingMessages.emit(event.message)

                    // Inspect for peer settlement card custom attributes
                    val extra = event.message.extraData
                    if (extra["is_settlement_card"] == true) {
                        val payload = PeerTransactionPayload(
                            transactionId = extra["tx_id"] as? String ?: "",
                            senderName = extra["tx_sender"] as? String ?: "",
                            recipientName = extra["tx_recipient"] as? String ?: "",
                            amount = (extra["tx_amount"] as? Number)?.toDouble() ?: 0.0,
                            currency = extra["tx_currency"] as? String ?: "KES",
                            purpose = extra["tx_purpose"] as? String ?: "",
                            status = runCatching { SettlementStatus.valueOf(extra["tx_status"] as String) }.getOrDefault(SettlementStatus.SETTLED),
                            timestampMs = (extra["tx_timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                            referenceCode = extra["tx_ref"] as? String ?: ""
                        )
                        _settlementEvents.emit(payload)
                    }
                }
            }
        }
    }

    private suspend inline fun <T : Any> suspendCancellableChatCall(
        crossinline block: () -> io.getstream.result.call.Call<T>
    ): Result<T> {
        val streamResult = block().await()
        return if (streamResult.isSuccess) {
            Result.success(streamResult.getOrThrow())
        } else {
            Result.failure(Exception(streamResult.errorOrNull()?.message ?: "Unknown Chat Error"))
        }
    }
}
