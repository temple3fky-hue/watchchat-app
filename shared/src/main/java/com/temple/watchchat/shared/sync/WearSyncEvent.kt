package com.temple.watchchat.shared.sync

import com.temple.watchchat.shared.model.Chat
import com.temple.watchchat.shared.model.Message
import kotlinx.serialization.Serializable

/**
 * 手机端和手表端之间同步用的事件。
 *
 * 第一版先定义统一消息格式，后续可以通过 Wear Data Layer、蓝牙同步，
 * 或者 Supabase 拉取后再转发给手表端。
 */
@Serializable
sealed class WearSyncEvent {
    @Serializable
    data class ChatListUpdated(
        val chats: List<Chat>,
    ) : WearSyncEvent()

    @Serializable
    data class ChatMessagesUpdated(
        val chatId: String,
        val messages: List<Message>,
    ) : WearSyncEvent()

    @Serializable
    data class NewMessageReceived(
        val chatId: String,
        val message: Message,
    ) : WearSyncEvent()

    @Serializable
    data class QuickReplyRequested(
        val chatId: String,
        val content: String,
    ) : WearSyncEvent()

    @Serializable
    data class VoiceTextReplyRequested(
        val chatId: String,
        val transcribedText: String,
    ) : WearSyncEvent()

    @Serializable
    data class MarkChatReadRequested(
        val chatId: String,
    ) : WearSyncEvent()
}
