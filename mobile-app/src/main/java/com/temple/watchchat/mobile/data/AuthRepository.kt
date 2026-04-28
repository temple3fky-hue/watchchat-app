package com.temple.watchchat.mobile.data

import android.content.Context
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email

object AuthRepository {
    private const val PREFS_NAME = "watchchat_auth"
    private const val KEY_DEMO_SESSION_ACTIVE = "demo_session_active"

    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    suspend fun signIn(
        email: String,
        password: String,
    ): AuthResult {
        val client = SupabaseClientProvider.client
            ?: return missingConfigOrDemoSuccess()

        return runCatching {
            client.auth.awaitInitialization()
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
            ?: return missingConfigOrDemoSuccess()

        return runCatching {
            client.auth.awaitInitialization()
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

    suspend fun signOut(): AuthResult {
        val client = SupabaseClientProvider.client
            ?: return if (SupabaseClientProvider.isDemoModeAllowed) {
                setDemoSessionActive(false)
                AuthResult.Success(isFakeAuth = true)
            } else {
                AuthResult.Error(SupabaseClientProvider.productionConfigError ?: "Supabase 未配置。")
            }

        return runCatching {
            client.auth.awaitInitialization()
            client.auth.signOut()
            setDemoSessionActive(false)
            AuthResult.Success(isFakeAuth = false)
        }.fold(
            onSuccess = { result -> result },
            onFailure = { error -> AuthResult.Error(error.toUserMessage()) },
        )
    }

    suspend fun hasActiveSession(): Boolean {
        val client = SupabaseClientProvider.client
            ?: return SupabaseClientProvider.isDemoModeAllowed && isDemoSessionActive()

        client.auth.awaitInitialization()
        return client.auth.currentSessionOrNull() != null
    }

    suspend fun currentUserId(): String? {
        val client = SupabaseClientProvider.client
            ?: return if (SupabaseClientProvider.isDemoModeAllowed && isDemoSessionActive()) "me" else null

        client.auth.awaitInitialization()
        return client.auth.currentSessionOrNull()?.user?.id
    }

    private fun missingConfigOrDemoSuccess(): AuthResult {
        return if (SupabaseClientProvider.isDemoModeAllowed) {
            setDemoSessionActive(true)
            AuthResult.Success(isFakeAuth = true)
        } else {
            AuthResult.Error(SupabaseClientProvider.productionConfigError ?: "正式模式需要配置 Supabase。")
        }
    }

    private fun isDemoSessionActive(): Boolean {
        return appContext
            ?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.getBoolean(KEY_DEMO_SESSION_ACTIVE, false)
            ?: false
    }

    private fun setDemoSessionActive(isActive: Boolean) {
        appContext
            ?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.edit()
            ?.putBoolean(KEY_DEMO_SESSION_ACTIVE, isActive)
            ?.apply()
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
    return message?.takeIf { it.isNotBlank() } ?: "操作失败，请稍后重试。"
}
