package com.temple.watchchat.wear.data

import com.temple.watchchat.wear.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object WearSupabaseClientProvider {
    val client: SupabaseClient? by lazy {
        val url = BuildConfig.SUPABASE_URL.trim()
        val anonKey = BuildConfig.SUPABASE_ANON_KEY.trim()

        if (url.isBlank() || anonKey.isBlank()) {
            null
        } else {
            createSupabaseClient(
                supabaseUrl = url,
                supabaseKey = anonKey,
            ) {
                install(Auth)
                install(Postgrest)
            }
        }
    }

    val isConfigured: Boolean
        get() = client != null
}
