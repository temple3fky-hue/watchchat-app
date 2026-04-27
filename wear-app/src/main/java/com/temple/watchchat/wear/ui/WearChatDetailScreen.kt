package com.temple.watchchat.wear.ui

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.temple.watchchat.shared.model.Chat
import com.temple.watchchat.shared.model.Message
import com.temple.watchchat.shared.sync.WearSyncEvent
import com.temple.watchchat.wear.data.WearFakeChatRepository
import com.temple.watchchat.wear.sync.WearSyncClient
import com.temple.watchchat.wear.util.WearVibration
import java.util.Locale

@Composable
fun WearChatDetailScreen(
    chat: Chat,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val messages = remember(chat.id) {
        mutableStateListOf<Message>().apply {
            addAll(WearFakeChatRepository.getMessages(chat.id))
        }
    }
    var statusText by remember(chat.id) { mutableStateOf("消息") }

    fun sendQuickReply(
        text: String,
        isVoiceReply: Boolean = false,
    ) {
        val message = WearFakeChatRepository.sendQuickReply(
            chatId = chat.id,
            content = text,
        )
        messages.add(message)
        statusText = if (isVoiceReply) {
            "语音已发送：$text"
        } else {
            "已回复：$text"
        }
        WearVibration.success(context)

        if (isVoiceReply) {
            WearSyncClient.sendVoiceTextReplyRequested(
                context = context,
                event = WearSyncEvent.VoiceTextReplyRequested(
                    chatId = chat.id,
                    transcribedText = text,
                ),
            )
        } else {
            WearSyncClient.sendQuickReplyRequested(
                context = context,
                event = WearSyncEvent.QuickReplyRequested(
                    chatId = chat.id,
                    content = text,
                ),
            )
        }
    }

    val voiceInputLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()

            if (spokenText.isNullOrEmpty()) {
                statusText = "未识别到语音"
                WearVibration.error(context)
            } else {
                sendQuickReply(
                    text = spokenText,
                    isVoiceReply = true,
                )
            }
        } else {
            statusText = "已取消语音输入"
            WearVibration.error(context)
        }
    }

    fun startVoiceInput() {
        statusText = "正在打开语音输入..."
        WearVibration.tap(context)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "说出要回复的内容")
        }

        runCatching {
            voiceInputLauncher.launch(intent)
        }.onFailure {
            statusText = "当前设备不支持语音输入"
            WearVibration.error(context)
        }
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
                        text = statusText,
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

            QuickReplyPanel(
                onReplyClick = { reply -> sendQuickReply(reply) },
                onVoiceClick = { startVoiceInput() },
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
private fun QuickReplyPanel(
    onReplyClick: (String) -> Unit,
    onVoiceClick: () -> Unit,
) {
    val firstRowReplies = listOf("好的", "收到", "等一下")
    val secondRowReplies = listOf("马上", "在忙")

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            firstRowReplies.forEach { reply ->
                QuickReplyButton(
                    text = reply,
                    modifier = Modifier.weight(1f),
                    onClick = { onReplyClick(reply) },
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            secondRowReplies.forEach { reply ->
                QuickReplyButton(
                    text = reply,
                    modifier = Modifier.weight(1f),
                    onClick = { onReplyClick(reply) },
                )
            }

            QuickReplyButton(
                text = "语音",
                modifier = Modifier.weight(1f),
                onClick = onVoiceClick,
            )
        }
    }
}

@Composable
private fun QuickReplyButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(
        modifier = modifier,
        onClick = onClick,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
