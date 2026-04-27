package com.temple.watchchat.mobile.data

import com.temple.watchchat.shared.model.Chat
import com.temple.watchchat.shared.model.Message
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

interface ChatRepository {
    suspend fun getChats(): List<Chat>

    suspend fun createChat(
        title: String,
        otherUserEmail: String,
    ): Chat

    suspend fun getMessages(chatId: String): List<Message>

    fun observeMessageChanges(chatId: String): Flow<Unit>

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
    private val localChats = FakeChatRepository.getChats().toMutableList()
    private val localMessagesByChatId = localChats.associate { chat ->
        chat.id to FakeChatRepository.getMessages(chat.id).toMutableList()
    }.toMutableMap()

    override suspend fun getChats(): List<Chat> {
        return localChats.sortedByDescending { chat -> chat.updatedAtMillis }
    }

    override suspend fun createChat(
        title: String,
        otherUserEmail: String,
    ): Chat {
        val now = System.currentTimeMillis()
        val cleanTitle = title.trim().ifBlank { "新的聊天" }
        val cleanEmail = otherUserEmail.trim()
        val participants = buildList {
            add("me")
            if (cleanEmail.isNotEmpty()) {
                add(cleanEmail)
            }
        }

        val chat = Chat(
            id = "local_chat_$now",
            title = cleanTitle,
            participantIds = participants,
            lastMessagePreview = if (cleanEmail.isNotEmpty()) {
                "已添加：$cleanEmail"
            } else {
                "本地新建聊天"
            },
            updatedAtMillis = now,
            unreadCount = 0,
        )

        localChats.add(0, chat)
        localMessagesByChatId[chat.id] = mutableListOf()
        return chat
    }

    override suspend fun getMessages(chatId: String): List<Message> {
        return localMessagesByChatId[chatId]?.toList().orEmpty()
    }

    override fun observeMessageChanges(chatId: String): Flow<Unit> {
        return emptyFlow()
    }

    override suspend fun sendTextMessage(
        chatId: String,
        content: String,
    ): Message {
        val message = FakeChatRepository.createLocalTextMessage(
            chatId = chatId,
            content = content,
        )

        localMessagesByChatId.getOrPut(chatId) { mutableListOf() }.add(message)

        val chatIndex = localChats.indexOfFirst { chat -> chat.id == chatId }
        if (chatIndex >= 0) {
            localChats[chatIndex] = localChats[chatIndex].copy(
                lastMessagePreview = content,
                updatedAtMillis = message.createdAtMillis,
                unreadCount = 0,
            )
        }

        return message
    }
}
