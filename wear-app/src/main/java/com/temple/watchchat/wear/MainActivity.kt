package com.temple.watchchat.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.temple.watchchat.shared.model.Chat
import com.temple.watchchat.wear.ui.WearChatDetailScreen
import com.temple.watchchat.wear.ui.WearChatListScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                var selectedChat by remember { mutableStateOf<Chat?>(null) }

                if (selectedChat == null) {
                    WearChatListScreen(
                        onChatClick = { chat -> selectedChat = chat },
                    )
                } else {
                    WearChatDetailScreen(
                        chat = selectedChat!!,
                        onBack = { selectedChat = null },
                    )
                }
            }
        }
    }
}
