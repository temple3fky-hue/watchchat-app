package com.temple.watchchat.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.temple.watchchat.mobile.data.AuthRepository
import com.temple.watchchat.mobile.ui.AuthScreen
import com.temple.watchchat.mobile.ui.ChatDetailScreen
import com.temple.watchchat.mobile.ui.ChatListScreen
import com.temple.watchchat.shared.model.Chat
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                val scope = rememberCoroutineScope()
                var authState by remember { mutableStateOf(AuthState.Checking) }
                var selectedChat by remember { mutableStateOf<Chat?>(null) }

                LaunchedEffect(Unit) {
                    authState = if (AuthRepository.hasActiveSession()) {
                        AuthState.Authed
                    } else {
                        AuthState.Unauthed
                    }
                }

                fun signOut() {
                    scope.launch {
                        AuthRepository.signOut()
                        selectedChat = null
                        authState = AuthState.Unauthed
                    }
                }

                when (authState) {
                    AuthState.Checking -> {
                        LoadingScreen()
                    }

                    AuthState.Unauthed -> {
                        AuthScreen(
                            onAuthSuccess = {
                                selectedChat = null
                                authState = AuthState.Authed
                            },
                        )
                    }

                    AuthState.Authed -> {
                        if (selectedChat == null) {
                            ChatListScreen(
                                onChatClick = { chat -> selectedChat = chat },
                                onSignOutClick = { signOut() },
                            )
                        } else {
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
}

private enum class AuthState {
    Checking,
    Unauthed,
    Authed,
}

@Composable
private fun LoadingScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "WatchChat",
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                text = "正在检查登录状态...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
