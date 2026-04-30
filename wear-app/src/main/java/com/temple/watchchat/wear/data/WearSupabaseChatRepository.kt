package com.temple.watchchat.wear.data

import com.temple.watchchat.shared.model.Chat
import com.temple.watchchat.shared.model.Message
import com.temple.watchchat.shared.model.MessageStatus
import com.temple.watchchat.shared.model.MessageType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import java.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object WearSupabaseChatRepository {
    suspend fun sendTextMessage(
        chatId: String,
        content: String,
    ): Message {
        val targetChatId = chatId.trim()
        val cleanContent = content.trim()
        if (targetChatId.isBlank() || cleanContent.isBlank()) {
            return failedMessage(chatId = targetChatId.ifBlank { chatId }, content = cleanContent)
        }

        val client = WearSupabaseClientProvider.client
            ?: return failedMessage(chatId = targetChatId, content = cleanContent)
        val currentUserId = client.auth.currentSessionOrNull()?.user?.id
            ?: return failedMessage(chatId = targetChatId, content = cleanContent)

        return runCatching {
            val inserted = client.from("messages")
                .insert(
                    MessageInsertDto(
                        chatId = targetChatId,
                        senderId = currentUserId,
                        content = cleanContent,
                    ),
                ) {
                    select()
                }
                .decodeSingle<MessageDetailDto>()

            inserted.toMessage()
        }.getOrElse {
            failedMessage(chatId = targetChatId, content = cleanContent)
        }
    }
    /**
     * 第一阶段只读取最近聊天列表。
     *
     * 依赖手表端本地已恢复的 Supabase Auth Session；
     * 未配置 / 未登录 / 查询失败时返回空列表，由上层回退到本地假数据。
     */
    suspend fun getRecentChats(): List<Chat> {
        val client = WearSupabaseClientProvider.client ?: return emptyList()
        val currentUserId = client.auth.currentSessionOrNull()?.user?.id ?: return emptyList()

        return runCatching {
            val myMemberships = client.from("chat_members")
                .select()
                .decodeList<ChatMemberDto>()
                .filter { member -> member.userId == currentUserId }

            val myChatIds = myMemberships.map { member -> member.chatId }.toSet()
            if (myChatIds.isEmpty()) return@runCatching emptyList()

            val chats = client.from("chats")
                .select()
                .decodeList<ChatDto>()
                .filter { chat -> chat.id in myChatIds }

            val allMembers = client.from("chat_members")
                .select()
                .decodeList<ChatMemberDto>()
                .filter { member -> member.chatId in myChatIds }

            val memberUserIds = allMembers.map { member -> member.userId }.toSet()
            val profilesById = client.from("profiles")
                .select()
                .decodeList<ProfileDto>()
                .filter { profile -> profile.id in memberUserIds }
                .associateBy { profile -> profile.id }

            val messagesByChatId = client.from("messages")
                .select()
                .decodeList<MessageDto>()
                .filter { message -> message.chatId in myChatIds }
                .groupBy { message -> message.chatId }

            chats
                .map { chat ->
                    val members = allMembers.filter { member -> member.chatId == chat.id }
                    val otherMemberNames = members
                        .mapNotNull { member -> profilesById[member.userId] }
                        .filter { profile -> profile.id != currentUserId }
                        .map { profile -> profile.displayName.ifBlank { profile.email } }

                    val lastMessage = messagesByChatId[chat.id]
                        ?.maxByOrNull { message -> message.createdAt ?: "" }
                    val updatedMillis = chat.updatedAt.toEpochMillis()
                        ?: chat.createdAt.toEpochMillis()
                        ?: lastMessage?.createdAt.toEpochMillis()
                        ?: 0L

                    Chat(
                        id = chat.id,
                        title = chat.title.ifBlank {
                            otherMemberNames.joinToString().ifBlank { "聊天 ${chat.id.takeLast(4)}" }
                        },
                        participantIds = members.map { member -> member.userId },
                        lastMessagePreview = lastMessage?.content?.ifBlank { "暂无消息" } ?: "暂无消息",
                        updatedAtMillis = updatedMillis,
                        unreadCount = 0,
                    )
                }
                .sortedByDescending { chat -> chat.updatedAtMillis }
        }.getOrElse {
            emptyList()
        }
    }

    /**
     * 第一阶段：只读取消息列表。
     *
     * 返回 null 代表需要上层回退到本地假消息：
     * - Supabase 未配置
     * - 没有 Auth Session
     * - 查询/解析失败
     *
     * 返回非 null（包括空列表）表示读取成功。
     */
    suspend fun getMessages(chatId: String): List<Message>? {
        val client = WearSupabaseClientProvider.client ?: return null
        val targetChatId = chatId.trim()
        if (targetChatId.isBlank()) return emptyList()
        if (client.auth.currentSessionOrNull()?.user?.id == null) return null

        return runCatching {
            client.from("messages")
                .select()
                .decodeList<MessageDetailDto>()
                .filter { message -> message.chatId == targetChatId }
                .sortedBy { message -> message.createdAt ?: "" }
                .map { message -> message.toMessage() }
        }.getOrNull()
    }
}

private fun failedMessage(
    chatId: String,
    content: String,
): Message {
    return Message(
        id = "wear_failed_${System.currentTimeMillis()}",
        chatId = chatId,
        senderId = "me",
        content = content,
        type = MessageType.TEXT,
        status = MessageStatus.FAILED,
        createdAtMillis = System.currentTimeMillis(),
    )
}

private fun String?.toEpochMillis(): Long? {
    if (this.isNullOrBlank()) return null
    return runCatching { Instant.parse(this).toEpochMilli() }.getOrNull()
}

@Serializable
private data class ChatDto(
    val id: String,
    val title: String = "",
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
)

@Serializable
private data class ChatMemberDto(
    @SerialName("chat_id")
    val chatId: String,
    @SerialName("user_id")
    val userId: String,
)

@Serializable
private data class ProfileDto(
    val id: String,
    val email: String = "",
    @SerialName("display_name")
    val displayName: String = "",
)

@Serializable
private data class MessageDto(
    val id: String,
    @SerialName("chat_id")
    val chatId: String,
    val content: String = "",
    @SerialName("created_at")
    val createdAt: String? = null,
)

@Serializable
private data class MessageDetailDto(
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
            createdAtMillis = createdAt.toEpochMillis() ?: 0L,
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
