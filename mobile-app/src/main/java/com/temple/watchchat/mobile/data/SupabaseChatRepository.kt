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

    override suspend fun createChat(
        title: String,
        otherUserEmail: String,
    ): Chat {
        val client = SupabaseClientProvider.client ?: return createLocalFallbackChat(title, otherUserEmail)
        val currentUserId = client.auth.currentSessionOrNull()?.user?.id
            ?: return createLocalFallbackChat(title, otherUserEmail)
        val cleanTitle = title.trim().ifBlank { "新的聊天" }
        val cleanEmail = otherUserEmail.trim()

        return runCatching {
            val otherProfile = if (cleanEmail.isNotEmpty()) {
                client.from("profiles")
                    .select()
                    .decodeList<ProfileDto>()
                    .firstOrNull { it.email.equals(cleanEmail, ignoreCase = true) }
            } else {
                null
            }

            val createdChat = client.from("chats")
                .insert(
                    ChatInsertDto(
                        title = cleanTitle,
                        createdBy = currentUserId,
                    ),
                ) {
                    select()
                }
                .decodeSingle<ChatDto>()

            client.from("chat_members").insert(
                ChatMemberInsertDto(
                    chatId = createdChat.id,
                    userId = currentUserId,
                ),
            )

            if (otherProfile != null && otherProfile.id != currentUserId) {
                client.from("chat_members").insert(
                    ChatMemberInsertDto(
                        chatId = createdChat.id,
                        userId = otherProfile.id,
                    ),
                )
            }

            val participants = buildList {
                add(currentUserId)
                if (otherProfile != null && otherProfile.id != currentUserId) {
                    add(otherProfile.id)
                }
            }

            Chat(
                id = createdChat.id,
                title = createdChat.title.ifBlank { cleanTitle },
                participantIds = participants,
                lastMessagePreview = when {
                    cleanEmail.isBlank() -> "新建聊天成功"
                    otherProfile == null -> "未找到该邮箱，已创建单人聊天"
                    otherProfile.id == currentUserId -> "这是你自己的邮箱，已创建单人聊天"
                    else -> "已添加：${otherProfile.displayName.ifBlank { otherProfile.email }}"
                },
                unreadCount = 0,
            )
        }.getOrElse {
            createLocalFallbackChat(cleanTitle, cleanEmail)
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

    private fun createLocalFallbackChat(
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

        return Chat(
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
    }
}

@Serializable
private data class ProfileDto(
    val id: String,
    val email: String,
    @SerialName("display_name")
    val displayName: String = "",
)

@Serializable
private data class ChatDto(
    val id: String,
    val title: String = "",
    @SerialName("created_by")
    val createdBy: String,
)

@Serializable
private data class ChatInsertDto(
    val title: String,
    @SerialName("created_by")
    val createdBy: String,
)

@Serializable
private data class ChatMemberDto(
    @SerialName("chat_id")
    val chatId: String,
    @SerialName("user_id")
    val userId: String,
)

@Serializable
private data class ChatMemberInsertDto(
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
