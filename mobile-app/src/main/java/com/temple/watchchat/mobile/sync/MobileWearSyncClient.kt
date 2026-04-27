package com.temple.watchchat.mobile.sync

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable
import com.temple.watchchat.shared.sync.WearSyncEvent
import com.temple.watchchat.shared.sync.WearSyncJson
import com.temple.watchchat.shared.sync.WearSyncPaths

/**
 * 手机端主动发送数据到手表端的客户端骨架。
 */
object MobileWearSyncClient {
    fun sendChatListUpdated(
        context: Context,
        event: WearSyncEvent.ChatListUpdated,
    ) {
        sendToConnectedNodes(
            context = context,
            path = WearSyncPaths.CHAT_LIST_UPDATED,
            event = event,
        )
    }

    fun sendChatMessagesUpdated(
        context: Context,
        event: WearSyncEvent.ChatMessagesUpdated,
    ) {
        sendToConnectedNodes(
            context = context,
            path = WearSyncPaths.CHAT_MESSAGES_UPDATED,
            event = event,
        )
    }

    fun sendNewMessageReceived(
        context: Context,
        event: WearSyncEvent.NewMessageReceived,
    ) {
        sendToConnectedNodes(
            context = context,
            path = WearSyncPaths.NEW_MESSAGE_RECEIVED,
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
                            Log.d(TAG, "Sent wear sync event to ${node.displayName}: $path")
                        }
                        .addOnFailureListener { error ->
                            Log.w(TAG, "Failed to send wear sync event: ${error.message}")
                        }
                }
            }
            .addOnFailureListener { error ->
                Log.w(TAG, "Failed to load connected wear nodes: ${error.message}")
            }
    }

    private const val TAG = "MobileWearSyncClient"
}
