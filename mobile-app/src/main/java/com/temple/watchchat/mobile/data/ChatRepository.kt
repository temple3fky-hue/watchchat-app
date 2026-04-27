package com.temple.watchchat.mobile.data

import com.temple.watchchat.shared.model.Chat
import com.temple.watchchat.shared.model.Message

interface ChatRepository {
    suspend fun getChats(): List<Chat>

    suspend fun createChat(
        title: String,
        otherUserEmail: String,
    ): Chat

    suspend fun getMessages(chatId: String): List<Message>

    suspend fun sendTextMessage(
        chatId: String,
        content: String,
    ): Message
}

object ChatRepositoryProvider {
    fun current(): ChatRepository {
        return if (SupabaseClientProvider.isConfigured) {
            SupabaseChatRepository
        } else {
            LocalChatRepository
        }
    }
}

private object LocalChatRepository : ChatRepository {
    override suspend fun getChats(): List<Chat> {
        return FakeChatRepository.getChats()
    }

    override suspend fun createChat(
        title: String,
        otherUserEmail: String,
    ): Chat {
        val now = System.currentTimeMillis()
        val cleanTitle = title.trim().ifBlank { "新的聊天" }
        val participants = buildList {
            add("me")
            if (otherUserEmail.trim().isNotEmpty()) {
                add(otherUserEmail.trim())
            }
        }

        return Chat(
            id = "local_chat_$now",
            title = cleanTitle,
            participantIds = participants,
            lastMessagePreview = if (otherUserEmail.trim().isNotEmpty()) {
                "已添加：${otherUserEmail.trim()}"
            } else {
                "本地新建聊天"
            },
            updatedAtMillis = now,
            unreadCount = 0,
        )
    }

    override suspend fun getMessages(chatId: String): List<Message> {
        return FakeChatRepository.getMessages(chatId)
    }

    override suspend fun sendTextMessage(
        chatId: String,
        content: String,
    ): Message {
        return FakeChatRepository.createLocalTextMessage(
            chatId = chatId,
            content = content,
        )
    }
}
