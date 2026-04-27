package com.temple.watchchat.shared.model

/**
 * 聊天消息。
 */
data class Message(
    val id: String,
    val chatId: String,
    val senderId: String,
    val content: String,
    val type: MessageType = MessageType.TEXT,
    val status: MessageStatus = MessageStatus.SENDING,
    val createdAtMillis: Long = 0L,
)

enum class MessageType {
    TEXT,
    VOICE,
}

enum class MessageStatus {
    SENDING,
    SENT,
    FAILED,
    READ,
}
