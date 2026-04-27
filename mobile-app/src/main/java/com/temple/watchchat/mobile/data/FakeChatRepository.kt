package com.temple.watchchat.mobile.data

import com.temple.watchchat.shared.model.Chat
import com.temple.watchchat.shared.model.Message
import com.temple.watchchat.shared.model.MessageStatus

/**
 * 本地假数据仓库。
 *
 * 现在先给界面提供测试数据。
 * 后续接入 Supabase 时，可以把这里替换成真正的远程数据仓库。
 */
object FakeChatRepository {
    fun getChats(): List<Chat> {
        return listOf(
            Chat(
                id = "chat_001",
                title = "小明",
                participantIds = listOf("me", "user_001"),
                lastMessagePreview = "晚上一起吃饭吗？",
                unreadCount = 2,
            ),
            Chat(
                id = "chat_002",
                title = "阿强",
                participantIds = listOf("me", "user_002"),
                lastMessagePreview = "收到，我马上过去。",
                unreadCount = 0,
            ),
            Chat(
                id = "chat_003",
                title = "家人",
                participantIds = listOf("me", "user_003"),
                lastMessagePreview = "路上注意安全。",
                unreadCount = 1,
            ),
        )
    }

    fun getMessages(chatId: String): List<Message> {
        return when (chatId) {
            "chat_001" -> listOf(
                Message(
                    id = "msg_001",
                    chatId = chatId,
                    senderId = "user_001",
                    content = "你今天几点下班？",
                    status = MessageStatus.READ,
                ),
                Message(
                    id = "msg_002",
                    chatId = chatId,
                    senderId = "me",
                    content = "大概 6 点半。",
                    status = MessageStatus.READ,
                ),
                Message(
                    id = "msg_003",
                    chatId = chatId,
                    senderId = "user_001",
                    content = "晚上一起吃饭吗？",
                    status = MessageStatus.READ,
                ),
            )

            "chat_002" -> listOf(
                Message(
                    id = "msg_004",
                    chatId = chatId,
                    senderId = "user_002",
                    content = "工具我已经放车上了。",
                    status = MessageStatus.READ,
                ),
                Message(
                    id = "msg_005",
                    chatId = chatId,
                    senderId = "me",
                    content = "收到，我马上过去。",
                    status = MessageStatus.SENT,
                ),
            )

            else -> listOf(
                Message(
                    id = "msg_006",
                    chatId = chatId,
                    senderId = "user_003",
                    content = "路上注意安全。",
                    status = MessageStatus.READ,
                ),
                Message(
                    id = "msg_007",
                    chatId = chatId,
                    senderId = "me",
                    content = "好的，到了告诉你。",
                    status = MessageStatus.SENT,
                ),
            )
        }
    }

    fun createLocalTextMessage(
        chatId: String,
        content: String,
    ): Message {
        val now = System.currentTimeMillis()
        return Message(
            id = "local_$now",
            chatId = chatId,
            senderId = "me",
            content = content,
            status = MessageStatus.SENT,
            createdAtMillis = now,
        )
    }
}
