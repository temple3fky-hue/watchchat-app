package com.temple.watchchat.shared.model

import kotlinx.serialization.Serializable

/**
 * 一对一聊天会话。
 */
@Serializable
data class Chat(
    val id: String,
    val title: String,
    val participantIds: List<String>,
    val lastMessagePreview: String = "",
    val updatedAtMillis: Long = 0L,
    val unreadCount: Int = 0,
)
