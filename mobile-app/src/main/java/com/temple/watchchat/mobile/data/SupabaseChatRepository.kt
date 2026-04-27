package com.temple.watchchat.mobile.data

import com.temple.watchchat.shared.model.Chat
import com.temple.watchchat.shared.model.Message
import com.temple.watchchat.shared.model.MessageStatus
import com.temple.watchchat.shared.model.MessageType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import java.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object SupabaseChatRepository : ChatRepository {
    override suspend fun getChats(): List<Chat> {
        val client = SupabaseClientProvider.client ?: return FakeChatRepository.getChats()
        val currentUserId = client.auth.currentSessionOrNull()?.user?.id
            ?: return FakeChatRepository.getChats()

        return runCatching {
            val myMemberships = client.from("chat_members")
                .select()
                .decodeList<ChatMemberDto>()
                .filter { it.userId == currentUserId }

            val myChatIds = myMemberships.map { it.chatId }.toSet()
            if (myChatIds.isEmpty()) return@runCatching emptyList()

            val chats = client.from("chats")
                .select()
                .decodeList<ChatDto>()
                .filter { it.id in myChatIds }

            val allMembers = client.from("chat_members")
                .select()
                .decodeList<ChatMemberDto>()
                .filter { it.chatId in myChatIds }

            val memberUserIds = allMembers.map { it.userId }.toSet()
            val profilesById = client.from("profiles")
                .select()
                .decodeList<ProfileDto>()
                .filter { it.id in memberUserIds }
                .associateBy { it.id }

            val messagesByChatId = client.from("messages")
                .select()
                .decodeList<MessageDto>()
                .filter { it.chatId in myChatIds }
                .groupBy { it.chatId }

            chats
                .sortedByDescending { it.updatedAt ?: it.createdAt ?: "" }
                .map { chatDto ->
                    val members = allMembers.filter { it.chatId == chatDto.id }
                    val participantIds = members.map { it.userId }
                    val otherMemberNames = members
                        .mapNotNull { member -> profilesById[member.userId] }
                        .filter { profile -> profile.id != currentUserId }
                        .map { profile -> profile.displayName.ifBlank { profile.email } }

                    val lastMessage = messagesByChatId[chatDto.id]
                        ?.maxByOrNull { it.createdAt ?: "" }

                    Chat(
                        id = chatDto.id,
                        title = chatDto.title.ifBlank {
                            otherMemberNames.joinToString().ifBlank {
                                "聊天 ${chatDto.id.takeLast(4)}"
                            }
                        },
                        participantIds = participantIds,
                        lastMessagePreview = lastMessage?.previewText() ?: "暂无消息",
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

    override fun observeMessageChanges(chatId: String): Flow<Unit> {
        return flow {
            while (true) {
                delay(3_000)
                emit(Unit)
            }
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

    override suspend fun markChatRead(chatId: String) {
        val client = SupabaseClientProvider.client ?: return

        runCatching {
            client.from("messages").update(
                MessageReadUpdateDto(
                    status = "read",
                    readAt = Instant.now().toString(),
                ),
            ) {
                filter {
                    eq("chat_id", chatId)
                }
            }
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
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
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

    fun previewText(): String {
        return if (messageType == "voice") {
            "[语音消息]"
        } else {
            content.ifBlank { "[空消息]" }
        }
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

@Serializable
private data class MessageReadUpdateDto(
    val status: String,
    @SerialName("read_at")
    val readAt: String,
)
