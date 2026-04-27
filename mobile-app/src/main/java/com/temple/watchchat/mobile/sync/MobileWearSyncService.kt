package com.temple.watchchat.mobile.sync

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.temple.watchchat.shared.sync.WearSyncEvent
import com.temple.watchchat.shared.sync.WearSyncJson
import com.temple.watchchat.shared.sync.WearSyncPaths

/**
 * 手机端 Wear Data Layer 同步服务骨架。
 *
 * 负责接收手表端发来的快捷回复、语音转文字回复、标记已读等请求。
 * 后续会在这里调用 ChatRepository，把手表请求真正写入手机端 / Supabase。
 */
class MobileWearSyncService : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        super.onMessageReceived(messageEvent)

        val rawPayload = messageEvent.data.decodeToString()
        val event = runCatching { WearSyncJson.decode(rawPayload) }
            .getOrElse { error ->
                Log.w(TAG, "Failed to decode wear sync event: ${error.message}")
                return
            }

        when (messageEvent.path) {
            WearSyncPaths.QUICK_REPLY_REQUESTED -> handleQuickReply(event)
            WearSyncPaths.VOICE_TEXT_REPLY_REQUESTED -> handleVoiceTextReply(event)
            WearSyncPaths.MARK_CHAT_READ_REQUESTED -> handleMarkChatRead(event)
            else -> Log.d(TAG, "Unhandled wear sync path: ${messageEvent.path}")
        }
    }

    private fun handleQuickReply(event: WearSyncEvent) {
        val request = event as? WearSyncEvent.QuickReplyRequested ?: return
        Log.d(TAG, "Quick reply requested: chatId=${request.chatId}, content=${request.content}")
        // TODO: 调用 ChatRepositoryProvider.current().sendTextMessage(request.chatId, request.content)
    }

    private fun handleVoiceTextReply(event: WearSyncEvent) {
        val request = event as? WearSyncEvent.VoiceTextReplyRequested ?: return
        Log.d(TAG, "Voice text reply requested: chatId=${request.chatId}, text=${request.transcribedText}")
        // TODO: 调用 ChatRepositoryProvider.current().sendTextMessage(request.chatId, request.transcribedText)
    }

    private fun handleMarkChatRead(event: WearSyncEvent) {
        val request = event as? WearSyncEvent.MarkChatReadRequested ?: return
        Log.d(TAG, "Mark chat read requested: chatId=${request.chatId}")
        // TODO: 后续接入已读状态更新。
    }

    private companion object {
        private const val TAG = "MobileWearSync"
    }
}
