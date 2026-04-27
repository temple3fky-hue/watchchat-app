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
        }.fold(
            onSuccess = { AuthResult.Success(isFakeAuth = false) },
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
            client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }

            // 第一版先只完成 Auth 注册。
            // 用户资料 profiles 表会在下一步接入 Postgrest 后写入。
            displayName.trim()
        }.fold(
            onSuccess = { AuthResult.Success(isFakeAuth = false) },
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

    data class Error(
        val message: String,
    ) : AuthResult()
}

private fun Throwable.toUserMessage(): String {
    return message?.takeIf { it.isNotBlank() } ?: "登录失败，请稍后重试。"
}
