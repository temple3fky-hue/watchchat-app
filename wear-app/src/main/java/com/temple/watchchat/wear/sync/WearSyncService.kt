package com.temple.watchchat.wear.sync

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.temple.watchchat.shared.sync.WearSyncEvent
import com.temple.watchchat.shared.sync.WearSyncJson
import com.temple.watchchat.shared.sync.WearSyncPaths
import com.temple.watchchat.wear.data.WearFakeChatRepository
import com.temple.watchchat.wear.util.WearVibration

/**
 * 手表端 Wear Data Layer 同步服务。
 *
 * 负责接收手机端推送的聊天列表、聊天消息和新消息提醒，
 * 并写入手表端本地缓存，驱动 UI 刷新。
 */
class WearSyncService : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)

        val rawPayload = messageEvent.data.decodeToString()
        val event = runCatching { WearSyncJson.decode(rawPayload) }
            .getOrElse { error ->
                Log.w(TAG, "Failed to decode mobile sync event: ${error.message}")
                return
            }

        when (messageEvent.path) {
            WearSyncPaths.CHAT_LIST_UPDATED -> handleChatListUpdated(event)
            WearSyncPaths.CHAT_MESSAGES_UPDATED -> handleChatMessagesUpdated(event)
            WearSyncPaths.NEW_MESSAGE_RECEIVED -> handleNewMessageReceived(event)
            else -> Log.d(TAG, "Unhandled mobile sync path: ${messageEvent.path}")
        }
    }

    private fun handleChatListUpdated(event: WearSyncEvent) {
        val update = event as? WearSyncEvent.ChatListUpdated ?: return
        WearFakeChatRepository.replaceChats(update.chats)
        Log.d(TAG, "Chat list updated from phone: count=${update.chats.size}")
    }

    private fun handleChatMessagesUpdated(event: WearSyncEvent) {
        val update = event as? WearSyncEvent.ChatMessagesUpdated ?: return
        WearFakeChatRepository.replaceMessages(
            chatId = update.chatId,
            messages = update.messages,
        )
        Log.d(TAG, "Chat messages updated from phone: chatId=${update.chatId}, count=${update.messages.size}")
    }

    private fun handleNewMessageReceived(event: WearSyncEvent) {
        val update = event as? WearSyncEvent.NewMessageReceived ?: return
        WearFakeChatRepository.addIncomingMessage(
            chatId = update.chatId,
            message = update.message,
        )
        WearVibration.incomingMessage(this)
        Log.d(TAG, "New message from phone: chatId=${update.chatId}, messageId=${update.message.id}")
    }

    private companion object {
        private const val TAG = "WearSyncService"
    }
}
