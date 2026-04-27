package com.temple.watchchat.shared

enum class MessageType {
    TEXT,
    VOICE
}

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val type: MessageType,
    val text: String? = null,
    val audioUrl: String? = null,
    val durationSeconds: Int? = null,
    val createdAt: String
)

data class Conversation(
    val id: String,
    val title: String,
    val lastMessagePreview: String,
    val updatedAt: String
)
