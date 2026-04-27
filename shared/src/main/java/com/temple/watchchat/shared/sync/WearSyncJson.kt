package com.temple.watchchat.shared.sync

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object WearSyncJson {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "event_type"
    }

    fun encode(event: WearSyncEvent): String {
        return json.encodeToString(event)
    }

    fun decode(raw: String): WearSyncEvent {
        return json.decodeFromString(raw)
    }
}
