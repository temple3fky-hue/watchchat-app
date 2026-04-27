package com.temple.watchchat.shared.model

import kotlinx.serialization.Serializable

/**
 * 聊天消息。
 */
@Serializable
data class Message(
    val id: String,
    val chatId: String,
    val senderId: String,
    val content: String,
    val type: MessageType = MessageType.TEXT,
    val status: MessageStatus = MessageStatus.SENDING,
    val createdAtMillis: Long = 0L,
)

@Serializable
enum class MessageType {
    TEXT,
    VOICE,
}

@Serializable
enum class MessageStatus {
    SENDING,
    SENT,
    FAILED,
    READ,
}
