package com.temple.watchchat.mobile.sync

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.temple.watchchat.mobile.data.ChatRepositoryProvider
import com.temple.watchchat.shared.sync.WearSyncEvent
import com.temple.watchchat.shared.sync.WearSyncJson
import com.temple.watchchat.shared.sync.WearSyncPaths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 手机端 Wear Data Layer 同步服务。
 *
 * 负责接收手表端发来的聊天同步、快捷回复、语音转文字回复、标记已读等请求。
 */
class MobileWearSyncService : WearableListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)

        val rawPayload = messageEvent.data.decodeToString()
        val event = runCatching { WearSyncJson.decode(rawPayload) }
            .getOrElse { error ->
                Log.w(TAG, "Failed to decode wear sync event: ${error.message}")
                return
            }

        when (messageEvent.path) {
            WearSyncPaths.CHAT_LIST_SYNC_REQUESTED -> handleChatListSyncRequest()
            WearSyncPaths.CHAT_MESSAGES_SYNC_REQUESTED -> handleChatMessagesSyncRequest(event)
            WearSyncPaths.QUICK_REPLY_REQUESTED -> handleQuickReply(event)
            WearSyncPaths.VOICE_TEXT_REPLY_REQUESTED -> handleVoiceTextReply(event)
            WearSyncPaths.MARK_CHAT_READ_REQUESTED -> handleMarkChatRead(event)
            else -> Log.d(TAG, "Unhandled wear sync path: ${messageEvent.path}")
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun handleChatListSyncRequest() {
        serviceScope.launch {
            runCatching {
                val repository = ChatRepositoryProvider.current()
                val latestChats = repository.getChats()
                MobileWearSyncClient.sendChatListUpdated(
                    context = this@MobileWearSyncService,
                    event = WearSyncEvent.ChatListUpdated(chats = latestChats),
                )
            }.onSuccess {
                Log.d(TAG, "Chat list sync request handled")
            }.onFailure { error ->
                Log.w(TAG, "Failed to handle chat list sync request: ${error.message}")
            }
        }
    }

    private fun handleChatMessagesSyncRequest(event: WearSyncEvent) {
        val request = event as? WearSyncEvent.ChatMessagesSyncRequested ?: return

        serviceScope.launch {
            runCatching {
                val repository = ChatRepositoryProvider.current()
                val latestMessages = repository.getMessages(request.chatId)
                MobileWearSyncClient.sendChatMessagesUpdated(
                    context = this@MobileWearSyncService,
                    event = WearSyncEvent.ChatMessagesUpdated(
                        chatId = request.chatId,
                        messages = latestMessages,
                    ),
                )
            }.onSuccess {
                Log.d(TAG, "Chat messages sync request handled: chatId=${request.chatId}")
            }.onFailure { error ->
                Log.w(TAG, "Failed to handle chat messages sync request: ${error.message}")
            }
        }
    }

    private fun handleQuickReply(event: WearSyncEvent) {
        val request = event as? WearSyncEvent.QuickReplyRequested ?: return
        sendTextFromWear(
            chatId = request.chatId,
            content = request.content,
            source = "quick_reply",
        )
    }

    private fun handleVoiceTextReply(event: WearSyncEvent) {
        val request = event as? WearSyncEvent.VoiceTextReplyRequested ?: return
        sendTextFromWear(
            chatId = request.chatId,
            content = request.transcribedText,
            source = "voice_text_reply",
        )
    }

    private fun handleMarkChatRead(event: WearSyncEvent) {
        val request = event as? WearSyncEvent.MarkChatReadRequested ?: return

        serviceScope.launch {
            runCatching {
                val repository = ChatRepositoryProvider.current()
                repository.markChatRead(request.chatId)

                val latestMessages = repository.getMessages(request.chatId)
                MobileWearSyncClient.sendChatMessagesUpdated(
                    context = this@MobileWearSyncService,
                    event = WearSyncEvent.ChatMessagesUpdated(
                        chatId = request.chatId,
                        messages = latestMessages,
                    ),
                )

                val latestChats = repository.getChats()
                MobileWearSyncClient.sendChatListUpdated(
                    context = this@MobileWearSyncService,
                    event = WearSyncEvent.ChatListUpdated(chats = latestChats),
                )
            }.onSuccess {
                Log.d(TAG, "Mark chat read handled: chatId=${request.chatId}")
            }.onFailure { error ->
                Log.w(TAG, "Failed to mark chat read: ${error.message}")
            }
        }
    }

    private fun sendTextFromWear(
        chatId: String,
        content: String,
        source: String,
    ) {
        val cleanContent = content.trim()
        if (cleanContent.isEmpty()) {
            Log.d(TAG, "Ignore empty wear reply: source=$source, chatId=$chatId")
            return
        }

        serviceScope.launch {
            runCatching {
                val repository = ChatRepositoryProvider.current()
                val sentMessage = repository.sendTextMessage(
                    chatId = chatId,
                    content = cleanContent,
                )

                MobileWearSyncClient.sendNewMessageReceived(
                    context = this@MobileWearSyncService,
                    event = WearSyncEvent.NewMessageReceived(
                        chatId = chatId,
                        message = sentMessage,
                    ),
                )

                val latestMessages = repository.getMessages(chatId)
                MobileWearSyncClient.sendChatMessagesUpdated(
                    context = this@MobileWearSyncService,
                    event = WearSyncEvent.ChatMessagesUpdated(
                        chatId = chatId,
                        messages = latestMessages,
                    ),
                )

                val latestChats = repository.getChats()
                MobileWearSyncClient.sendChatListUpdated(
                    context = this@MobileWearSyncService,
                    event = WearSyncEvent.ChatListUpdated(chats = latestChats),
                )
            }.onSuccess {
                Log.d(TAG, "Wear reply handled: source=$source, chatId=$chatId")
            }.onFailure { error ->
                Log.w(TAG, "Failed to handle wear reply: ${error.message}")
            }
        }
    }

    private companion object {
        private const val TAG = "MobileWearSync"
    }
}
