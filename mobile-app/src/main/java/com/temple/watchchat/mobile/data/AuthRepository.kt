package com.temple.watchchat.mobile.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email

object AuthRepository {
    suspend fun signIn(
        email: String,
        password: String,
    ): AuthResult {
        val client = SupabaseClientProvider.client
            ?: return AuthResult.Success(isFakeAuth = true)

        return runCatching {
            client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }

            if (client.auth.currentSessionOrNull() == null) {
                AuthResult.Error("登录成功但没有获取到会话，请重新打开 App 后再试。")
            } else {
                AuthResult.Success(isFakeAuth = false)
            }
        }.fold(
            onSuccess = { result -> result },
            onFailure = { error -> AuthResult.Error(error.toUserMessage()) },
        )
    }

    suspend fun signUp(
        displayName: String,
        email: String,
        password: String,
    ): AuthResult {
        val client = SupabaseClientProvider.client
            ?: return AuthResult.Success(isFakeAuth = true)

        return runCatching {
            val cleanEmail = email.trim()
            val cleanDisplayName = displayName.trim()

            client.auth.signUpWith(Email) {
                this.email = cleanEmail
                this.password = password
            }

            val user = client.auth.currentSessionOrNull()?.user
            if (user == null) {
                AuthResult.NeedsEmailConfirmation("注册成功，请先打开邮箱完成验证，然后再返回登录。")
            } else {
                ProfileRepository.upsertProfile(
                    userId = user.id,
                    email = cleanEmail,
                    displayName = cleanDisplayName,
                )
                AuthResult.Success(isFakeAuth = false)
            }
        }.fold(
            onSuccess = { result -> result },
            onFailure = { error -> AuthResult.Error(error.toUserMessage()) },
        )
    }

    suspend fun hasActiveSession(): Boolean {
        val client = SupabaseClientProvider.client ?: return false
        return client.auth.currentSessionOrNull() != null
    }
}

sealed class AuthResult {
    data class Success(
        val isFakeAuth: Boolean,
    ) : AuthResult()

    data class NeedsEmailConfirmation(
        val message: String,
    ) : AuthResult()

    data class Error(
        val message: String,
    ) : AuthResult()
}

private fun Throwable.toUserMessage(): String {
    return message?.takeIf { it.isNotBlank() } ?: "登录失败，请稍后重试。"
}
