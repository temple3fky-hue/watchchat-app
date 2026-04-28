package com.temple.watchchat.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.temple.watchchat.mobile.data.ChatRepositoryProvider
import com.temple.watchchat.mobile.data.FriendRequest
import com.temple.watchchat.mobile.data.FriendRepositoryProvider
import com.temple.watchchat.mobile.data.FriendUser
import com.temple.watchchat.mobile.sync.MobileWearSyncClient
import com.temple.watchchat.shared.model.Chat
import com.temple.watchchat.shared.sync.WearSyncEvent
import kotlinx.coroutines.launch

@Composable
fun ChatListScreen(
    onChatClick: (Chat) -> Unit,
    onSignOutClick: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var chats by remember { mutableStateOf<List<Chat>>(emptyList()) }
    var friends by remember { mutableStateOf<List<FriendUser>>(emptyList()) }
    var incomingRequests by remember { mutableStateOf<List<FriendRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isCreating by remember { mutableStateOf(false) }
    var isFriendBusy by remember { mutableStateOf(false) }
    var newChatTitle by remember { mutableStateOf("") }
    var otherUserEmail by remember { mutableStateOf("") }
    var friendEmail by remember { mutableStateOf("") }
    var friendStatus by remember { mutableStateOf("输入邮箱可以发送好友申请") }

    fun syncChatsToWear(updatedChats: List<Chat>) {
        MobileWearSyncClient.sendChatListUpdated(
            context = context,
            event = WearSyncEvent.ChatListUpdated(chats = updatedChats),
        )
    }

    suspend fun reloadChats() {
        isLoading = true
        val latestChats = ChatRepositoryProvider.current().getChats()
        chats = latestChats
        syncChatsToWear(latestChats)
        isLoading = false
    }

    suspend fun reloadFriends() {
        friends = FriendRepositoryProvider.current().getFriends()
        incomingRequests = FriendRepositoryProvider.current().getIncomingRequests()
    }

    fun createChat(
        titleOverride: String? = null,
        emailOverride: String? = null,
        openAfterCreate: Boolean = false,
    ) {
        val title = (titleOverride ?: newChatTitle).trim().ifBlank { "新的聊天" }
        val email = (emailOverride ?: otherUserEmail).trim()
        if (isCreating) return

        isCreating = true
        scope.launch {
            val newChat = ChatRepositoryProvider.current().createChat(
                title = title,
                otherUserEmail = email,
            )
            val updatedChats = listOf(newChat) + chats
            chats = updatedChats
            syncChatsToWear(updatedChats)
            newChatTitle = ""
            otherUserEmail = ""
            isCreating = false
            if (openAfterCreate) {
                onChatClick(newChat)
            }
        }
    }

    fun sendFriendRequest() {
        if (isFriendBusy) return
        isFriendBusy = true
        friendStatus = "正在发送好友申请..."
        scope.launch {
            val result = FriendRepositoryProvider.current().sendFriendRequest(friendEmail)
            friendStatus = result.message
            if (result.success) {
                friendEmail = ""
            }
            reloadFriends()
            isFriendBusy = false
        }
    }

    fun acceptFriendRequest(requestId: String) {
        if (isFriendBusy) return
        isFriendBusy = true
        friendStatus = "正在同意好友申请..."
        scope.launch {
            val result = FriendRepositoryProvider.current().acceptRequest(requestId)
            friendStatus = result.message
            reloadFriends()
            isFriendBusy = false
        }
    }

    fun rejectFriendRequest(requestId: String) {
        if (isFriendBusy) return
        isFriendBusy = true
        friendStatus = "正在拒绝好友申请..."
        scope.launch {
            val result = FriendRepositoryProvider.current().rejectRequest(requestId)
            friendStatus = result.message
            reloadFriends()
            isFriendBusy = false
        }
    }

    fun startChatWithFriend(friend: FriendUser) {
        createChat(
            titleOverride = friend.title,
            emailOverride = friend.email,
            openAfterCreate = true,
        )
    }

    LaunchedEffect(Unit) {
        reloadChats()
        reloadFriends()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "WatchChat",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = if (isLoading) "正在加载聊天..." else "最近聊天",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Button(onClick = onSignOutClick) {
                    Text(text = "退出")
                }
            }

            Spacer(modifier = Modifier.size(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    AddFriendPanel(
                        friendEmail = friendEmail,
                        onFriendEmailChange = { friendEmail = it },
                        statusText = friendStatus,
                        enabled = !isFriendBusy,
                        onSendClick = { sendFriendRequest() },
                    )
                }

                if (incomingRequests.isNotEmpty()) {
                    item {
                        SectionTitle(text = "好友申请")
                    }
                    items(
                        items = incomingRequests,
                        key = { request -> request.id },
                    ) { request ->
                        FriendRequestItem(
                            request = request,
                            enabled = !isFriendBusy,
                            onAcceptClick = { acceptFriendRequest(request.id) },
                            onRejectClick = { rejectFriendRequest(request.id) },
                        )
                    }
                }

                item {
                    SectionTitle(text = "好友")
                }

                if (friends.isEmpty()) {
                    item {
                        Text(
                            text = "暂无好友。输入对方邮箱发送好友申请，对方同意后会显示在这里。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    items(
                        items = friends,
                        key = { friend -> friend.userId },
                    ) { friend ->
                        FriendListItem(
                            friend = friend,
                            enabled = !isCreating,
                            onChatClick = { startChatWithFriend(friend) },
                        )
                    }
                }

                item {
                    SectionTitle(text = "新建聊天")
                }

                item {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = newChatTitle,
                        onValueChange = { newChatTitle = it },
                        label = { Text(text = "新聊天名称") },
                        singleLine = true,
                        enabled = !isCreating,
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            modifier = Modifier.weight(1f),
                            value = otherUserEmail,
                            onValueChange = { otherUserEmail = it },
                            label = { Text(text = "对方邮箱，可选") },
                            singleLine = true,
                            enabled = !isCreating,
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Button(
                            onClick = { createChat() },
                            enabled = !isCreating,
                        ) {
                            Text(text = if (isCreating) "创建中" else "新建")
                        }
                    }
                }

                item {
                    SectionTitle(text = "最近聊天")
                }

                if (!isLoading && chats.isEmpty()) {
                    item {
                        Text(
                            text = "暂无聊天。输入名称后点击新建；也可以填写对方邮箱。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    items(
                        items = chats,
                        key = { chat -> chat.id },
                    ) { chat ->
                        ChatListItem(
                            chat = chat,
                            onClick = { onChatClick(chat) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddFriendPanel(
    friendEmail: String,
    onFriendEmailChange: (String) -> Unit,
    statusText: String,
    enabled: Boolean,
    onSendClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            Text(
                text = "添加好友",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.size(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = friendEmail,
                    onValueChange = onFriendEmailChange,
                    label = { Text(text = "好友邮箱") },
                    singleLine = true,
                    enabled = enabled,
                )
                Spacer(modifier = Modifier.size(8.dp))
                Button(
                    onClick = onSendClick,
                    enabled = enabled,
                ) {
                    Text(text = "申请")
                }
            }
            Spacer(modifier = Modifier.size(6.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun FriendRequestItem(
    request: FriendRequest,
    enabled: Boolean,
    onAcceptClick: () -> Unit,
    onRejectClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
        ) {
            Text(
                text = request.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = request.requesterEmail.ifBlank { "请求添加你为好友" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.size(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onAcceptClick,
                    enabled = enabled,
                ) {
                    Text(text = "同意")
                }
                TextButton(
                    onClick = onRejectClick,
                    enabled = enabled,
                ) {
                    Text(text = "拒绝")
                }
            }
        }
    }
}

@Composable
private fun FriendListItem(
    friend: FriendUser,
    enabled: Boolean,
    onChatClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = friend.title.firstOrNull()?.toString() ?: "友",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Spacer(modifier = Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = friend.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = friend.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Button(
                onClick = onChatClick,
                enabled = enabled,
            ) {
                Text(text = "聊天")
            }
        }
    }
}

@Composable
private fun ChatListItem(
    chat: Chat,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = chat.title.firstOrNull()?.toString() ?: "聊",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            Spacer(modifier = Modifier.size(12.dp))

            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = chat.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = chat.lastMessagePreview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (chat.unreadCount > 0) {
                Badge {
                    Text(text = chat.unreadCount.toString())
                }
            }
        }
    }
}
