package com.temple.watchchat.wear.data

import com.temple.watchchat.shared.model.Chat

/**
 * 手表端本地假数据。
 *
 * 第一版先让 Wear OS 界面跑起来。
 * 后续会通过手机同步或 Supabase 拉取真实聊天。
 */
object WearFakeChatRepository {
    fun getRecentChats(): List<Chat> {
        return listOf(
            Chat(
                id = "wear_chat_001",
                title = "小明",
                participantIds = listOf("me", "user_001"),
                lastMessagePreview = "晚上一起吃饭吗？",
                unreadCount = 2,
            ),
            Chat(
                id = "wear_chat_002",
                title = "阿强",
                participantIds = listOf("me", "user_002"),
                lastMessagePreview = "收到，我马上过去。",
                unreadCount = 0,
            ),
            Chat(
                id = "wear_chat_003",
                title = "家人",
                participantIds = listOf("me", "user_003"),
                lastMessagePreview = "路上注意安全。",
                unreadCount = 1,
            ),
        )
    }
}
