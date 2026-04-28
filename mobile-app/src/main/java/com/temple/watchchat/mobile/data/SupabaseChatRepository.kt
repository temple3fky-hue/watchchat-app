package com.temple.watchchat.mobile.data

import com.temple.watchchat.shared.model.Chat
import com.temple.watchchat.shared.model.Message
import com.temple.watchchat.shared.model.MessageStatus
import com.temple.watchchat.shared.model.MessageType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import java.time.Instant
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

object SupabaseChatRepository : ChatRepository {
    override suspend fun getChats(): List<Chat> {
        val client = SupabaseClientProvider.client ?: return emptyList()
        val currentUserId = client.auth.currentSessionOrNull()?.user?.id ?: return emptyList()

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
            emptyList()
        }
    }

    override suspend fun createChat(
        title: String,
        otherUserEmail: String,
    ): Chat {
        val client = SupabaseClientProvider.client
            ?: return failedChat(title, "Supabase 未配置")
        val currentUserId = client.auth.currentSessionOrNull()?.user?.id
            ?: return failedChat(title, "请先登录")
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

            if (otherProfile != null && otherProfile.id != currentUserId) {
                val existingChat = findExistingDirectChat(
                    currentUserId = currentUserId,
                    otherProfile = otherProfile,
                )
                if (existingChat != null) {
                    return@runCatching existingChat
                }
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
                updatedAtMillis = System.currentTimeMillis(),
            )
        }.getOrElse { error ->
            failedChat(cleanTitle, error.message ?: "创建失败")
        }
    }

    private suspend fun findExistingDirectChat(
        currentUserId: String,
        otherProfile: ProfileDto,
    ): Chat? {
        val client = SupabaseClientProvider.client ?: return null

        val myMemberships = client.from("chat_members")
            .select()
            .decodeList<ChatMemberDto>()
            .filter { member -> member.userId == currentUserId }

        val myChatIds = myMemberships.map { member -> member.chatId }.toSet()
        if (myChatIds.isEmpty()) return null

        val allMembers = client.from("chat_members")
            .select()
            .decodeList<ChatMemberDto>()
            .filter { member -> member.chatId in myChatIds }

        val existingChatId = allMembers
            .groupBy { member -> member.chatId }
            .entries
            .firstOrNull { entry ->
                val userIds = entry.value.map { member -> member.userId }.toSet()
                currentUserId in userIds && otherProfile.id in userIds
            }
            ?.key
            ?: return null

        val existingChatDto = client.from("chats")
            .select()
            .decodeList<ChatDto>()
            .firstOrNull { chat -> chat.id == existingChatId }
            ?: return null

        val latestMessage = client.from("messages")
            .select()
            .decodeList<MessageDto>()
            .filter { message -> message.chatId == existingChatId }
            .maxByOrNull { message -> message.createdAt ?: "" }

        return Chat(
            id = existingChatDto.id,
            title = existingChatDto.title.ifBlank {
                otherProfile.displayName.ifBlank { otherProfile.email }
            },
            participantIds = listOf(currentUserId, otherProfile.id),
            lastMessagePreview = latestMessage?.previewText() ?: "继续聊天",
            unreadCount = 0,
            updatedAtMillis = System.currentTimeMillis(),
        )
    }

    override suspend fun getMessages(chatId: String): List<Message> {
        val client = SupabaseClientProvider.client ?: return emptyList()

        return runCatching {
            client.from("messages")
                .select()
                .decodeList<MessageDto>()
                .filter { it.chatId == chatId }
                .sortedBy { it.createdAt ?: "" }
                .map { it.toMessage() }
        }.getOrElse {
            emptyList()
        }
    }

    override fun observeMessageChanges(chatId: String): Flow<Unit> {
        val client = SupabaseClientProvider.client ?: return emptyFlow()
        val currentUserId = client.auth.currentSessionOrNull()?.user?.id ?: return emptyFlow()
        val targetChatId = chatId.trim()
        if (targetChatId.isBlank()) return emptyFlow()

        return callbackFlow {
            val channelName = "messages-$currentUserId-$targetChatId-${System.currentTimeMillis()}"
            val channel = runCatching { client.channel(channelName) }.getOrElse {
                close(it)
                return@callbackFlow
            }

            val realtimeJob: Job = launch {
                runCatching {
                    channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                        table = "messages"
                        filter(column = "chat_id", operator = FilterOperator.EQ, value = targetChatId)
                    }.collect { action ->
                        val changedChatId = extractChatId(action)
                        if (changedChatId == targetChatId) {
                            trySend(Unit).isSuccess
                        }
                    }
                }.onFailure {
                    close(it)
                }
            }

            val subscribed = runCatching {
                channel.subscribe(blockUntilSubscribed = true)
            }.isSuccess

            if (!subscribed) {
                realtimeJob.cancel()
                runCatching { channel.unsubscribe() }
                close()
                return@callbackFlow
            }

            awaitClose {
                realtimeJob.cancel()
                runCatching { channel.unsubscribe() }
            }
        }.buffer(Channel.CONFLATED)
    }


    private fun extractChatId(action: PostgresAction): String? {
        val payload = when (action) {
            is PostgresAction.Insert -> action.record
            is PostgresAction.Update -> action.record
            is PostgresAction.Select -> action.record
            is PostgresAction.Delete -> action.oldRecord
        } ?: return null

        return payload["chat_id"]?.jsonPrimitive?.contentOrNull
    }

    override suspend fun sendTextMessage(
        chatId: String,
        content: String,
    ): Message {
        val client = SupabaseClientProvider.client
            ?: return failedMessage(chatId, content)
        val currentUserId = client.auth.currentSessionOrNull()?.user?.id
            ?: return failedMessage(chatId, content)

        return runCatching {
            client.from("messages").insert(
                MessageInsertDto(
                    chatId = chatId,
                    senderId = currentUserId,
                    content = content,
                ),
            )

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
            failedMessage(chatId, content)
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

    private fun failedChat(
        title: String,
        reason: String,
    ): Chat {
        val now = System.currentTimeMillis()
        return Chat(
            id = "failed_chat_$now",
            title = title.trim().ifBlank { "创建失败" },
            participantIds = emptyList(),
            lastMessagePreview = "创建失败：$reason",
            updatedAtMillis = now,
            unreadCount = 0,
        )
    }

    private fun failedMessage(
        chatId: String,
        content: String,
    ): Message {
        return Message(
            id = "failed_message_${System.currentTimeMillis()}",
            chatId = chatId,
            senderId = "me",
            content = content,
            type = MessageType.TEXT,
            status = MessageStatus.FAILED,
            createdAtMillis = System.currentTimeMillis(),
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
