package com.temple.watchchat.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.temple.watchchat.mobile.data.ChatRepositoryProvider
import com.temple.watchchat.mobile.sync.MobileWearSyncClient
import com.temple.watchchat.shared.model.Chat
import com.temple.watchchat.shared.sync.WearSyncEvent
import kotlinx.coroutines.launch

@Composable
fun ChatListScreen(
    onChatClick: (Chat) -> Unit,
    onSignOutClick: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var chats by remember { mutableStateOf<List<Chat>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isCreating by remember { mutableStateOf(false) }
    var newChatTitle by remember { mutableStateOf("") }
    var otherUserEmail by remember { mutableStateOf("") }

    fun syncChatsToWear(updatedChats: List<Chat>) {
        MobileWearSyncClient.sendChatListUpdated(
            context = context,
            event = WearSyncEvent.ChatListUpdated(chats = updatedChats),
        )
    }

    suspend fun reloadChats() {
        isLoading = true
        val latestChats = ChatRepositoryProvider.current().getChats()
        chats = latestChats
        syncChatsToWear(latestChats)
        isLoading = false
    }

    fun createChat() {
        val title = newChatTitle.trim().ifBlank { "新的聊天" }
        val email = otherUserEmail.trim()
        if (isCreating) return

        isCreating = true
        scope.launch {
            val newChat = ChatRepositoryProvider.current().createChat(
                title = title,
                otherUserEmail = email,
            )
            val updatedChats = listOf(newChat) + chats
            chats = updatedChats
            syncChatsToWear(updatedChats)
            newChatTitle = ""
            otherUserEmail = ""
            isCreating = false
        }
    }

    LaunchedEffect(Unit) {
        reloadChats()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "WatchChat",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (isLoading) "正在加载聊天..." else "最近聊天",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Button(onClick = onSignOutClick) {
                    Text(text = "退出")
                }
            }

            Spacer(modifier = Modifier.size(16.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = newChatTitle,
                onValueChange = { newChatTitle = it },
                label = { Text(text = "新聊天名称") },
                singleLine = true,
                enabled = !isCreating,
            )

            Spacer(modifier = Modifier.size(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = otherUserEmail,
                    onValueChange = { otherUserEmail = it },
                    label = { Text(text = "对方邮箱，可选") },
                    singleLine = true,
                    enabled = !isCreating,
                )
                Spacer(modifier = Modifier.size(8.dp))
                Button(
                    onClick = { createChat() },
                    enabled = !isCreating,
                ) {
                    Text(text = if (isCreating) "创建中" else "新建")
                }
            }

            Spacer(modifier = Modifier.size(16.dp))

            if (!isLoading && chats.isEmpty()) {
                Text(
                    text = "暂无聊天。输入名称后点击新建；也可以填写对方邮箱。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(
                        items = chats,
                        key = { chat -> chat.id },
                    ) { chat ->
                        ChatListItem(
                            chat = chat,
                            onClick = { onChatClick(chat) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatListItem(
    chat: Chat,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = chat.title.firstOrNull()?.toString() ?: "聊",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            Spacer(modifier = Modifier.size(12.dp))

            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = chat.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = chat.lastMessagePreview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (chat.unreadCount > 0) {
                Badge {
                    Text(text = chat.unreadCount.toString())
                }
            }
        }
    }
}
