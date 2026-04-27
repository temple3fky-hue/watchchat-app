package com.temple.watchchat.wear.data

import com.temple.watchchat.shared.model.Chat
import com.temple.watchchat.shared.model.Message
import com.temple.watchchat.shared.model.MessageStatus

/**
 * 手表端本地聊天缓存。
 *
 * 第一版先支持假数据和手机端同步数据混用。
 * 手机端通过 Wear Data Layer 推送数据后，会写入这里。
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

    private val syncedChats = mutableMapOf<String, Chat>()

    fun getRecentChats(): List<Chat> {
        return if (syncedChats.isNotEmpty()) {
            syncedChats.values
                .map { chat ->
                    val lastMessage = messagesByChatId[chat.id]?.lastOrNull()
                    chat.copy(
                        lastMessagePreview = lastMessage?.content ?: chat.lastMessagePreview,
                        unreadCount = unreadCounts[chat.id] ?: chat.unreadCount,
                        updatedAtMillis = lastMessage?.createdAtMillis ?: chat.updatedAtMillis,
                    )
                }
                .sortedByDescending { chat -> chat.updatedAtMillis }
        } else {
            listOf(
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
    }

    fun getMessages(chatId: String): List<Message> {
        return messagesByChatId[chatId]?.toList().orEmpty()
    }

    fun replaceChats(chats: List<Chat>) {
        syncedChats.clear()
        chats.forEach { chat ->
            syncedChats[chat.id] = chat.copy(
                unreadCount = unreadCounts[chat.id] ?: chat.unreadCount,
            )
        }
    }

    fun replaceMessages(
        chatId: String,
        messages: List<Message>,
    ) {
        messagesByChatId[chatId] = messages.toMutableList()
        val latest = messages.lastOrNull()
        val chat = syncedChats[chatId]
        if (chat != null && latest != null) {
            syncedChats[chatId] = chat.copy(
                lastMessagePreview = latest.content,
                updatedAtMillis = latest.createdAtMillis,
            )
        }
    }

    fun addIncomingMessage(
        chatId: String,
        message: Message,
    ) {
        val messages = messagesByChatId.getOrPut(chatId) { mutableListOf() }
        if (messages.none { it.id == message.id }) {
            messages.add(message)
        }

        unreadCounts[chatId] = (unreadCounts[chatId] ?: 0) + 1
        val chat = syncedChats[chatId]
        if (chat != null) {
            syncedChats[chatId] = chat.copy(
                lastMessagePreview = message.content,
                unreadCount = unreadCounts[chatId] ?: 0,
                updatedAtMillis = message.createdAtMillis,
            )
        }
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
        syncedChats[chatId]?.let { chat ->
            syncedChats[chatId] = chat.copy(
                lastMessagePreview = content,
                updatedAtMillis = now,
            )
        }
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

        addIncomingMessage(chatId, message)
        return message
    }

    fun markChatRead(chatId: String) {
        unreadCounts[chatId] = 0
        syncedChats[chatId]?.let { chat ->
            syncedChats[chatId] = chat.copy(unreadCount = 0)
        }
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
