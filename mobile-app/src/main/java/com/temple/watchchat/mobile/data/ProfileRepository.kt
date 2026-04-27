package com.temple.watchchat.mobile.data

import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object ProfileRepository {
    suspend fun upsertProfile(
        userId: String,
        email: String,
        displayName: String,
    ) {
        val client = SupabaseClientProvider.client ?: return

        client.from("profiles").upsert(
            ProfileUpsertDto(
                id = userId,
                email = email,
                displayName = displayName,
            ),
        )
    }
}

@Serializable
private data class ProfileUpsertDto(
    val id: String,
    val email: String,
    @SerialName("display_name")
    val displayName: String,
)
