package com.temple.watchchat.wear.sync

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable
import com.temple.watchchat.shared.sync.WearSyncEvent
import com.temple.watchchat.shared.sync.WearSyncJson
import com.temple.watchchat.shared.sync.WearSyncPaths

/**
 * 手表端主动发送请求到手机端的客户端骨架。
 */
object WearSyncClient {
    fun sendQuickReplyRequested(
        context: Context,
        event: WearSyncEvent.QuickReplyRequested,
    ) {
        sendToConnectedNodes(
            context = context,
            path = WearSyncPaths.QUICK_REPLY_REQUESTED,
            event = event,
        )
    }

    fun sendVoiceTextReplyRequested(
        context: Context,
        event: WearSyncEvent.VoiceTextReplyRequested,
    ) {
        sendToConnectedNodes(
            context = context,
            path = WearSyncPaths.VOICE_TEXT_REPLY_REQUESTED,
            event = event,
        )
    }

    fun sendMarkChatReadRequested(
        context: Context,
        event: WearSyncEvent.MarkChatReadRequested,
    ) {
        sendToConnectedNodes(
            context = context,
            path = WearSyncPaths.MARK_CHAT_READ_REQUESTED,
            event = event,
        )
    }

    private fun sendToConnectedNodes(
        context: Context,
        path: String,
        event: WearSyncEvent,
    ) {
        val payload = WearSyncJson.encode(event).encodeToByteArray()
        val nodeClient = Wearable.getNodeClient(context)
        val messageClient = Wearable.getMessageClient(context)

        nodeClient.connectedNodes
            .addOnSuccessListener { nodes ->
                nodes.forEach { node ->
                    messageClient.sendMessage(node.id, path, payload)
                        .addOnSuccessListener {
                            Log.d(TAG, "Sent phone sync event to ${node.displayName}: $path")
                        }
                        .addOnFailureListener { error ->
                            Log.w(TAG, "Failed to send phone sync event: ${error.message}")
                        }
                }
            }
            .addOnFailureListener { error ->
                Log.w(TAG, "Failed to load connected phone nodes: ${error.message}")
            }
    }

    private const val TAG = "WearSyncClient"
}
