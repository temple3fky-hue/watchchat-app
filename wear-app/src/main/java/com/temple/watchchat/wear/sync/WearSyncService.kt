package com.temple.watchchat.wear.sync

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.temple.watchchat.shared.sync.WearSyncEvent
import com.temple.watchchat.shared.sync.WearSyncJson
import com.temple.watchchat.shared.sync.WearSyncPaths

/**
 * 手表端 Wear Data Layer 同步服务骨架。
 *
 * 负责接收手机端推送的聊天列表、聊天消息和新消息提醒。
 * 后续会在这里把数据写入手表端本地仓库，再通知 UI 刷新。
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
        Log.d(TAG, "Chat list updated from phone: count=${update.chats.size}")
        // TODO: 写入手表端本地聊天列表缓存，并通知 UI 刷新。
    }

    private fun handleChatMessagesUpdated(event: WearSyncEvent) {
        val update = event as? WearSyncEvent.ChatMessagesUpdated ?: return
        Log.d(TAG, "Chat messages updated from phone: chatId=${update.chatId}, count=${update.messages.size}")
        // TODO: 写入手表端本地消息缓存，并通知 UI 刷新。
    }

    private fun handleNewMessageReceived(event: WearSyncEvent) {
        val update = event as? WearSyncEvent.NewMessageReceived ?: return
        Log.d(TAG, "New message from phone: chatId=${update.chatId}, messageId=${update.message.id}")
        // TODO: 写入新消息、增加未读数、触发新消息震动提醒。
    }

    private companion object {
        private const val TAG = "WearSyncService"
    }
}
