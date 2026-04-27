package com.temple.watchchat.mobile.ui

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.temple.watchchat.mobile.data.FakeChatRepository
import com.temple.watchchat.shared.model.Chat
import com.temple.watchchat.shared.model.Message
import com.temple.watchchat.shared.model.MessageStatus

@Composable
fun ChatDetailScreen(
    chat: Chat,
    onBack: () -> Unit,
) {
    val messages = remember(chat.id) { FakeChatRepository.getMessages(chat.id).toMutableStateList() }
    var inputText by remember(chat.id) { mutableStateOf("") }

    fun sendMessage() {
        val content = inputText.trim()
        if (content.isEmpty()) return

        messages.add(
            FakeChatRepository.createLocalTextMessage(
                chatId = chat.id,
                content = content,
            ),
        )
        inputText = ""
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
                TextButton(onClick = onBack) {
                    Text(text = "返回")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = chat.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "聊天详情",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.size(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(messages) { message ->
                    MessageBubble(message = message)
                }
            }

            Spacer(modifier = Modifier.size(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text(text = "输入消息...") },
                    singleLine = true,
                )
                Spacer(modifier = Modifier.size(8.dp))
                Button(
                    onClick = { sendMessage() },
                    enabled = inputText.trim().isNotEmpty(),
                ) {
                    Text(text = "发送")
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: Message) {
    val isMine = message.senderId == "me"
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
            modifier = Modifier.fillMaxWidth(0.78f),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor,
                )
                if (isMine) {
                    Text(
                        text = when (message.status) {
                            MessageStatus.SENDING -> "发送中"
                            MessageStatus.SENT -> "已发送"
                            MessageStatus.FAILED -> "发送失败"
                            MessageStatus.READ -> "已读"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor,
                    )
                }
            }
        }
    }
}
