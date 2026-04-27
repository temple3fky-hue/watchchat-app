package com.temple.watchchat.wear.data

import com.temple.watchchat.shared.model.Chat
import com.temple.watchchat.shared.model.Message
import com.temple.watchchat.shared.model.MessageStatus

/**
 * 手表端本地假数据。
 *
 * 第一版先让 Wear OS 界面跑起来。
 * 后续会通过手机同步或 Supabase 拉取真实聊天。
 */
object WearFakeChatRepository {
    private val messagesByChatId = mutableMapOf(
        "wear_chat_001" to mutableListOf(
            Message(
                id = "wear_msg_001",
                chatId = "wear_chat_001",
                senderId = "user_001",
                content = "你今天几点下班？",
                status = MessageStatus.READ,
            ),
            Message(
                id = "wear_msg_002",
                chatId = "wear_chat_001",
                senderId = "me",
                content = "大概 6 点半。",
                status = MessageStatus.READ,
            ),
            Message(
                id = "wear_msg_003",
                chatId = "wear_chat_001",
                senderId = "user_001",
                content = "晚上一起吃饭吗？",
                status = MessageStatus.READ,
            ),
        ),
        "wear_chat_002" to mutableListOf(
            Message(
                id = "wear_msg_004",
                chatId = "wear_chat_002",
                senderId = "user_002",
                content = "工具我已经放车上了。",
                status = MessageStatus.READ,
            ),
            Message(
                id = "wear_msg_005",
                chatId = "wear_chat_002",
                senderId = "me",
                content = "收到，我马上过去。",
                status = MessageStatus.SENT,
            ),
        ),
        "wear_chat_003" to mutableListOf(
            Message(
                id = "wear_msg_006",
                chatId = "wear_chat_003",
                senderId = "user_003",
                content = "路上注意安全。",
                status = MessageStatus.READ,
            ),
            Message(
                id = "wear_msg_007",
                chatId = "wear_chat_003",
                senderId = "me",
                content = "好的，到了告诉你。",
                status = MessageStatus.SENT,
            ),
        ),
    )

    private val unreadCounts = mutableMapOf(
        "wear_chat_001" to 2,
        "wear_chat_002" to 0,
        "wear_chat_003" to 1,
    )

    fun getRecentChats(): List<Chat> {
        return listOf(
            buildChat(
                id = "wear_chat_001",
                title = "小明",
                participantIds = listOf("me", "user_001"),
            ),
            buildChat(
                id = "wear_chat_002",
                title = "阿强",
                participantIds = listOf("me", "user_002"),
            ),
            buildChat(
                id = "wear_chat_003",
                title = "家人",
                participantIds = listOf("me", "user_003"),
            ),
        )
    }

    fun getMessages(chatId: String): List<Message> {
        return messagesByChatId[chatId]?.toList().orEmpty()
    }

    fun sendQuickReply(
        chatId: String,
        content: String,
    ): Message {
        val now = System.currentTimeMillis()
        val message = Message(
            id = "wear_local_$now",
            chatId = chatId,
            senderId = "me",
            content = content,
            status = MessageStatus.SENT,
            createdAtMillis = now,
        )

        messagesByChatId.getOrPut(chatId) { mutableListOf() }.add(message)
        return message
    }

    fun simulateIncomingMessage(chatId: String): Message {
        val now = System.currentTimeMillis()
        val message = Message(
            id = "wear_incoming_$now",
            chatId = chatId,
            senderId = "remote_user",
            content = "这是一条新的手表提醒消息",
            status = MessageStatus.READ,
            createdAtMillis = now,
        )

        messagesByChatId.getOrPut(chatId) { mutableListOf() }.add(message)
        unreadCounts[chatId] = (unreadCounts[chatId] ?: 0) + 1
        return message
    }

    fun markChatRead(chatId: String) {
        unreadCounts[chatId] = 0
    }

    private fun buildChat(
        id: String,
        title: String,
        participantIds: List<String>,
    ): Chat {
        return Chat(
            id = id,
            title = title,
            participantIds = participantIds,
            lastMessagePreview = messagesByChatId[id]?.lastOrNull()?.content.orEmpty(),
            unreadCount = unreadCounts[id] ?: 0,
        )
    }
}
