package com.temple.watchchat.wear.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.temple.watchchat.shared.model.Chat
import com.temple.watchchat.shared.model.Message
import com.temple.watchchat.wear.data.WearFakeChatRepository

@Composable
fun WearChatDetailScreen(
    chat: Chat,
    onBack: () -> Unit,
) {
    val messages = remember(chat.id) {
        mutableStateListOf<Message>().apply {
            addAll(WearFakeChatRepository.getMessages(chat.id))
        }
    }
    var lastReply by remember(chat.id) { mutableStateOf<String?>(null) }

    fun sendQuickReply(text: String) {
        val message = WearFakeChatRepository.sendQuickReply(
            chatId = chat.id,
            content = text,
        )
        messages.add(message)
        lastReply = "已回复：$text"
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onBack) {
                    Text(text = "返回")
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = chat.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = lastReply ?: "消息",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(modifier = Modifier.size(6.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(messages) { message ->
                    WearMessageBubble(message = message)
                }
            }

            Spacer(modifier = Modifier.size(6.dp))

            QuickReplyRow(
                onReplyClick = { reply -> sendQuickReply(reply) },
            )
        }
    }
}

@Composable
private fun WearMessageBubble(message: Message) {
    val isMine = message.senderId == "me" || message.senderId.startsWith("wear_local_")
    val rowArrangement = if (isMine) Arrangement.End else Arrangement.Start
    val bubbleColor = if (isMine) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isMine) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = rowArrangement,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.82f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
        ) {
            Text(
                modifier = Modifier.padding(10.dp),
                text = message.content,
                style = MaterialTheme.typography.bodySmall,
                color = textColor,
            )
        }
    }
}

@Composable
private fun QuickReplyRow(
    onReplyClick: (String) -> Unit,
) {
    val replies = listOf("好的", "收到", "等一下")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        replies.forEach { reply ->
            Button(
                modifier = Modifier.weight(1f),
                onClick = { onReplyClick(reply) },
            ) {
                Text(
                    text = reply,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}
