package com.temple.watchchat.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.temple.watchchat.mobile.ui.AuthScreen
import com.temple.watchchat.mobile.ui.ChatDetailScreen
import com.temple.watchchat.mobile.ui.ChatListScreen
import com.temple.watchchat.shared.model.Chat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                var isAuthed by remember { mutableStateOf(false) }
                var selectedChat by remember { mutableStateOf<Chat?>(null) }

                when {
                    !isAuthed -> {
                        AuthScreen(
                            onAuthSuccess = { isAuthed = true },
                        )
                    }

                    selectedChat == null -> {
                        ChatListScreen(
                            onChatClick = { chat -> selectedChat = chat },
                        )
                    }

                    else -> {
                        ChatDetailScreen(
                            chat = selectedChat!!,
                            onBack = { selectedChat = null },
                        )
                    }
                }
            }
        }
    }
}
