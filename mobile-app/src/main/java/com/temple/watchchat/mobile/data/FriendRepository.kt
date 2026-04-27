package com.temple.watchchat.mobile.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 好友系统第一版：
 * - 通过邮箱发送好友申请
 * - 查看好友列表
 * - 查看收到的申请
 * - 同意 / 拒绝申请
 */
interface FriendRepository {
    suspend fun getFriends(): List<FriendUser>

    suspend fun getIncomingRequests(): List<FriendRequest>

    suspend fun sendFriendRequest(email: String): FriendActionResult

    suspend fun acceptRequest(requestId: String): FriendActionResult

    suspend fun rejectRequest(requestId: String): FriendActionResult
}

data class FriendUser(
    val userId: String,
    val email: String,
    val displayName: String,
) {
    val title: String
        get() = displayName.ifBlank { email }
}

data class FriendRequest(
    val id: String,
    val requesterId: String,
    val requesterEmail: String,
    val requesterName: String,
) {
    val title: String
        get() = requesterName.ifBlank { requesterEmail }
}

data class FriendActionResult(
    val success: Boolean,
    val message: String,
)

object FriendRepositoryProvider {
    fun current(): FriendRepository {
        return if (SupabaseClientProvider.isConfigured) {
            SupabaseFriendRepository
        } else {
            LocalFriendRepository
        }
    }
}

private object SupabaseFriendRepository : FriendRepository {
    override suspend fun getFriends(): List<FriendUser> {
        val client = SupabaseClientProvider.client ?: return emptyList()
        val currentUserId = client.auth.currentSessionOrNull()?.user?.id ?: return emptyList()

        return runCatching {
            val friendships = client.from("friendships")
                .select()
                .decodeList<FriendshipDto>()
                .filter { friendship ->
                    friendship.status == "accepted" &&
                        (friendship.requesterId == currentUserId || friendship.addresseeId == currentUserId)
                }

            if (friendships.isEmpty()) return@runCatching emptyList()

            val friendIds = friendships.map { friendship ->
                if (friendship.requesterId == currentUserId) {
                    friendship.addresseeId
                } else {
                    friendship.requesterId
                }
            }.toSet()

            client.from("profiles")
                .select()
                .decodeList<ProfileDto>()
                .filter { profile -> profile.id in friendIds }
                .map { profile ->
                    FriendUser(
                        userId = profile.id,
                        email = profile.email,
                        displayName = profile.displayName,
                    )
                }
                .sortedBy { friend -> friend.title }
        }.getOrElse {
            emptyList()
        }
    }

    override suspend fun getIncomingRequests(): List<FriendRequest> {
        val client = SupabaseClientProvider.client ?: return emptyList()
        val currentUserId = client.auth.currentSessionOrNull()?.user?.id ?: return emptyList()

        return runCatching {
            val requests = client.from("friendships")
                .select()
                .decodeList<FriendshipDto>()
                .filter { friendship ->
                    friendship.addresseeId == currentUserId && friendship.status == "pending"
                }

            if (requests.isEmpty()) return@runCatching emptyList()

            val requesterIds = requests.map { request -> request.requesterId }.toSet()
            val profilesById = client.from("profiles")
                .select()
                .decodeList<ProfileDto>()
                .filter { profile -> profile.id in requesterIds }
                .associateBy { profile -> profile.id }

            requests.map { request ->
                val profile = profilesById[request.requesterId]
                FriendRequest(
                    id = request.id,
                    requesterId = request.requesterId,
                    requesterEmail = profile?.email.orEmpty(),
                    requesterName = profile?.displayName.orEmpty(),
                )
            }
        }.getOrElse {
            emptyList()
        }
    }

    override suspend fun sendFriendRequest(email: String): FriendActionResult {
        val client = SupabaseClientProvider.client
            ?: return FriendActionResult(false, "Supabase 未配置")
        val currentUserId = client.auth.currentSessionOrNull()?.user?.id
            ?: return FriendActionResult(false, "请先登录")
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank()) {
            return FriendActionResult(false, "请输入好友邮箱")
        }

        return runCatching {
            val targetProfile = client.from("profiles")
                .select()
                .decodeList<ProfileDto>()
                .firstOrNull { profile -> profile.email.equals(cleanEmail, ignoreCase = true) }
                ?: return@runCatching FriendActionResult(false, "没有找到这个邮箱")

            if (targetProfile.id == currentUserId) {
                return@runCatching FriendActionResult(false, "不能添加自己为好友")
            }

            val existing = client.from("friendships")
                .select()
                .decodeList<FriendshipDto>()
                .firstOrNull { friendship ->
                    (friendship.requesterId == currentUserId && friendship.addresseeId == targetProfile.id) ||
                        (friendship.requesterId == targetProfile.id && friendship.addresseeId == currentUserId)
                }

            when (existing?.status) {
                "accepted" -> return@runCatching FriendActionResult(false, "你们已经是好友")
                "pending" -> return@runCatching FriendActionResult(false, "好友申请已发送，等待对方同意")
                "rejected" -> {
                    client.from("friendships").update(
                        FriendshipStatusUpdateDto(status = "pending"),
                    ) {
                        filter {
                            eq("id", existing.id)
                        }
                    }
                    return@runCatching FriendActionResult(true, "已重新发送好友申请")
                }
            }

            client.from("friendships").insert(
                FriendshipInsertDto(
                    requesterId = currentUserId,
                    addresseeId = targetProfile.id,
                ),
            )

            FriendActionResult(true, "好友申请已发送")
        }.getOrElse { error ->
            FriendActionResult(false, "发送失败：${error.message ?: "未知错误"}")
        }
    }

    override suspend fun acceptRequest(requestId: String): FriendActionResult {
        return updateRequestStatus(
            requestId = requestId,
            status = "accepted",
            successMessage = "已同意好友申请",
        )
    }

    override suspend fun rejectRequest(requestId: String): FriendActionResult {
        return updateRequestStatus(
            requestId = requestId,
            status = "rejected",
            successMessage = "已拒绝好友申请",
        )
    }

    private suspend fun updateRequestStatus(
        requestId: String,
        status: String,
        successMessage: String,
    ): FriendActionResult {
        val client = SupabaseClientProvider.client
            ?: return FriendActionResult(false, "Supabase 未配置")
        val currentUserId = client.auth.currentSessionOrNull()?.user?.id
            ?: return FriendActionResult(false, "请先登录")

        return runCatching {
            val request = client.from("friendships")
                .select()
                .decodeList<FriendshipDto>()
                .firstOrNull { friendship ->
                    friendship.id == requestId && friendship.addresseeId == currentUserId
                }
                ?: return@runCatching FriendActionResult(false, "没有找到这条好友申请")

            client.from("friendships").update(
                FriendshipStatusUpdateDto(status = status),
            ) {
                filter {
                    eq("id", request.id)
                }
            }

            FriendActionResult(true, successMessage)
        }.getOrElse { error ->
            FriendActionResult(false, "操作失败：${error.message ?: "未知错误"}")
        }
    }
}

private object LocalFriendRepository : FriendRepository {
    override suspend fun getFriends(): List<FriendUser> = emptyList()

    override suspend fun getIncomingRequests(): List<FriendRequest> = emptyList()

    override suspend fun sendFriendRequest(email: String): FriendActionResult {
        return FriendActionResult(false, "本地模式暂不支持好友功能，请配置 Supabase")
    }

    override suspend fun acceptRequest(requestId: String): FriendActionResult {
        return FriendActionResult(false, "本地模式暂不支持好友功能")
    }

    override suspend fun rejectRequest(requestId: String): FriendActionResult {
        return FriendActionResult(false, "本地模式暂不支持好友功能")
    }
}

@Serializable
private data class ProfileDto(
    val id: String,
    val email: String,
    @SerialName("display_name")
    val displayName: String = "",
)

@Serializable
private data class FriendshipDto(
    val id: String,
    @SerialName("requester_id")
    val requesterId: String,
    @SerialName("addressee_id")
    val addresseeId: String,
    val status: String,
)

@Serializable
private data class FriendshipInsertDto(
    @SerialName("requester_id")
    val requesterId: String,
    @SerialName("addressee_id")
    val addresseeId: String,
    val status: String = "pending",
)

@Serializable
private data class FriendshipStatusUpdateDto(
    val status: String,
)
