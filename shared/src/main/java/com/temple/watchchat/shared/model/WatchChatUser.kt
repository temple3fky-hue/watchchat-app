package com.temple.watchchat.shared.model

import kotlinx.serialization.Serializable

/**
 * WatchChat 用户基础信息。
 *
 * 第一版先只保存聊天展示需要的字段。
 * 后续接入 Supabase Auth 后，id 可以对应 auth.users.id。
 */
@Serializable
data class WatchChatUser(
    val id: String,
    val email: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val createdAtMillis: Long = 0L,
)
