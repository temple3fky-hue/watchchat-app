package com.temple.watchchat.wear.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.temple.watchchat.shared.model.Chat
import com.temple.watchchat.shared.sync.WearSyncEvent
import com.temple.watchchat.wear.data.WearFakeChatRepository
import com.temple.watchchat.wear.sync.WearSyncClient
import com.temple.watchchat.wear.util.WearVibration

@Composable
fun WearChatListScreen(
    onChatClick: (Chat) -> Unit,
) {
    val context = LocalContext.current
    var chats by remember { mutableStateOf(WearFakeChatRepository.getRecentChats()) }
    var statusText by remember { mutableStateOf("最近聊天") }

    fun simulateIncomingMessage() {
        val targetChat = chats.firstOrNull() ?: return
        WearFakeChatRepository.simulateIncomingMessage(targetChat.id)
        chats = WearFakeChatRepository.getRecentChats()
        statusText = "收到新消息"
        WearVibration.incomingMessage(context)
    }

    fun openChat(chat: Chat) {
        WearFakeChatRepository.markChatRead(chat.id)
        chats = WearFakeChatRepository.getRecentChats()
        statusText = "已读：${chat.title}"

        WearSyncClient.sendMarkChatReadRequested(
            context = context,
            event = WearSyncEvent.MarkChatReadRequested(
                chatId = chat.id,
            ),
        )

        onChatClick(chat.copy(unreadCount = 0))
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "WatchChat",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.size(6.dp))

            Button(
                onClick = { simulateIncomingMessage() },
            ) {
                Text(
                    text = "模拟新消息",
                    style = MaterialTheme.typography.labelSmall,
                )
            }

            Spacer(modifier = Modifier.size(6.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(chats) { chat ->
                    WearChatListItem(
                        chat = chat,
                        onClick = { openChat(chat) },
                    )
                }
            }
        }
    }
}

@Composable
private fun WearChatListItem(
    chat: Chat,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = chat.title.firstOrNull()?.toString() ?: "聊",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            Spacer(modifier = Modifier.size(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chat.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = chat.lastMessagePreview,
                    style = MaterialTheme.typography.bodySmall,
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
