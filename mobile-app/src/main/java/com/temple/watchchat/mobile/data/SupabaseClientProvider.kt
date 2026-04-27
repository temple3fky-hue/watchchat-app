package com.temple.watchchat.mobile.data

import com.temple.watchchat.mobile.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

object SupabaseClientProvider {
    val client: SupabaseClient? by lazy {
        val url = BuildConfig.SUPABASE_URL
        val anonKey = BuildConfig.SUPABASE_ANON_KEY

        if (url.isBlank() || anonKey.isBlank()) {
            null
        } else {
            createSupabaseClient(
                supabaseUrl = url,
                supabaseKey = anonKey,
            ) {
                install(Auth)
                install(Postgrest)
                install(Realtime)
                install(Storage)
            }
        }
    }

    val isConfigured: Boolean
        get() = client != null
}
