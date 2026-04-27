package com.temple.watchchat.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.temple.watchchat.mobile.data.AuthRepository
import com.temple.watchchat.mobile.data.AuthResult
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var isRegisterMode by remember { mutableStateOf(false) }
    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var authMessage by remember { mutableStateOf<String?>(null) }

    val canSubmit = !isLoading &&
        email.trim().isNotEmpty() &&
        password.trim().length >= 6 &&
        (!isRegisterMode || displayName.trim().isNotEmpty())

    fun submitAuth() {
        if (!canSubmit) return

        isLoading = true
        authMessage = null

        scope.launch {
            val result = if (isRegisterMode) {
                AuthRepository.signUp(
                    displayName = displayName.trim(),
                    email = email.trim(),
                    password = password,
                )
            } else {
                AuthRepository.signIn(
                    email = email.trim(),
                    password = password,
                )
            }

            isLoading = false

            when (result) {
                is AuthResult.Success -> {
                    authMessage = if (result.isFakeAuth) {
                        "未配置 Supabase，已使用本地假登录进入。"
                    } else {
                        null
                    }
                    onAuthSuccess()
                }

                is AuthResult.Error -> {
                    authMessage = result.message
                }
            }
        }
    }

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
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (isRegisterMode) "创建你的腕聊账号" else "登录你的腕聊账号",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.size(28.dp))

            if (isRegisterMode) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text(text = "昵称") },
                    singleLine = true,
                    enabled = !isLoading,
                )
                Spacer(modifier = Modifier.size(12.dp))
            }

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = email,
                onValueChange = { email = it },
                label = { Text(text = "邮箱") },
                singleLine = true,
                enabled = !isLoading,
            )

            Spacer(modifier = Modifier.size(12.dp))

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = password,
                onValueChange = { password = it },
                label = { Text(text = "密码，至少 6 位") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                enabled = !isLoading,
            )

            Spacer(modifier = Modifier.size(20.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { submitAuth() },
                enabled = canSubmit,
            ) {
                Text(
                    text = when {
                        isLoading -> "处理中..."
                        isRegisterMode -> "注册并进入"
                        else -> "登录"
                    },
                )
            }

            TextButton(
                enabled = !isLoading,
                onClick = {
                    isRegisterMode = !isRegisterMode
                    authMessage = null
                },
            ) {
                Text(
                    text = if (isRegisterMode) {
                        "已有账号？去登录"
                    } else {
                        "没有账号？去注册"
                    },
                )
            }

            Spacer(modifier = Modifier.size(12.dp))

            Text(
                text = authMessage ?: "配置 Supabase 后会使用真实 Auth；未配置时走本地假登录。",
                style = MaterialTheme.typography.bodySmall,
                color = if (authMessage == null) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                },
            )
        }
    }
}
