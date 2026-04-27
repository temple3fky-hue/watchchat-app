package com.temple.watchchat.mobile.data

import com.temple.watchchat.shared.model.Chat
import com.temple.watchchat.shared.model.Message
import com.temple.watchchat.shared.model.MessageStatus
import com.temple.watchchat.shared.model.MessageType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object SupabaseChatRepository : ChatRepository {
    override suspend fun getChats(): List<Chat> {
        val client = SupabaseClientProvider.client ?: return FakeChatRepository.getChats()
        val currentUserId = client.auth.currentSessionOrNull()?.user?.id
            ?: return FakeChatRepository.getChats()

        return runCatching {
            val memberships = client.from("chat_members")
                .select()
                .decodeList<ChatMemberDto>()
                .filter { it.userId == currentUserId }

            memberships.map { member ->
                Chat(
                    id = member.chatId,
                    title = "聊天 ${member.chatId.takeLast(4)}",
                    participantIds = listOf(currentUserId),
                    lastMessagePreview = "点击进入聊天",
                    unreadCount = 0,
                )
            }
        }.getOrElse {
            FakeChatRepository.getChats()
        }
    }

    override suspend fun getMessages(chatId: String): List<Message> {
        val client = SupabaseClientProvider.client ?: return FakeChatRepository.getMessages(chatId)

        return runCatching {
            client.from("messages")
                .select()
                .decodeList<MessageDto>()
                .filter { it.chatId == chatId }
                .sortedBy { it.createdAt ?: "" }
                .map { it.toMessage() }
        }.getOrElse {
            FakeChatRepository.getMessages(chatId)
        }
    }

    override suspend fun sendTextMessage(
        chatId: String,
        content: String,
    ): Message {
        val client = SupabaseClientProvider.client ?: return FakeChatRepository.createLocalTextMessage(chatId, content)
        val currentUserId = client.auth.currentSessionOrNull()?.user?.id
            ?: return FakeChatRepository.createLocalTextMessage(chatId, content)

        return runCatching {
            val newMessage = MessageInsertDto(
                chatId = chatId,
                senderId = currentUserId,
                content = content,
            )

            client.from("messages").insert(newMessage)

            Message(
                id = "local_${System.currentTimeMillis()}",
                chatId = chatId,
                senderId = currentUserId,
                content = content,
                type = MessageType.TEXT,
                status = MessageStatus.SENT,
                createdAtMillis = System.currentTimeMillis(),
            )
        }.getOrElse {
            FakeChatRepository.createLocalTextMessage(chatId, content)
        }
    }
}

@Serializable
private data class ChatMemberDto(
    @SerialName("chat_id")
    val chatId: String,
    @SerialName("user_id")
    val userId: String,
)

@Serializable
private data class MessageDto(
    val id: String,
    @SerialName("chat_id")
    val chatId: String,
    @SerialName("sender_id")
    val senderId: String,
    val content: String = "",
    @SerialName("message_type")
    val messageType: String = "text",
    val status: String = "sent",
    @SerialName("created_at")
    val createdAt: String? = null,
) {
    fun toMessage(): Message {
        return Message(
            id = id,
            chatId = chatId,
            senderId = senderId,
            content = content,
            type = if (messageType == "voice") MessageType.VOICE else MessageType.TEXT,
            status = when (status) {
                "sending" -> MessageStatus.SENDING
                "failed" -> MessageStatus.FAILED
                "read" -> MessageStatus.READ
                else -> MessageStatus.SENT
            },
        )
    }
}

@Serializable
private data class MessageInsertDto(
    @SerialName("chat_id")
    val chatId: String,
    @SerialName("sender_id")
    val senderId: String,
    val content: String,
    @SerialName("message_type")
    val messageType: String = "text",
    val status: String = "sent",
)
