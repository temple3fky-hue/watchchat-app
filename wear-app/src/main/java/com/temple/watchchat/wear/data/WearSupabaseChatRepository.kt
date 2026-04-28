package com.temple.watchchat.wear.data

import com.temple.watchchat.shared.model.Chat
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import java.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object WearSupabaseChatRepository {
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
