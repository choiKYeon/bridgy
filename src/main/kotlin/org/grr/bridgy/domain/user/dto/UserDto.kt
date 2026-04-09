package org.grr.bridgy.domain.user.dto

import org.grr.bridgy.domain.user.entity.User
import org.grr.bridgy.domain.user.entity.UserRole
import java.time.LocalDateTime

data class SignUpRequest(
    val email: String,
    val password: String,
    val nickname: String,
    val bio: String? = null
)

data class UpdateUserRequest(
    val nickname: String? = null,
    val profileImageUrl: String? = null,
    val bio: String? = null
)

data class UserResponse(
    val id: Long,
    val email: String,
    val nickname: String,
    val role: UserRole,
    val profileImageUrl: String?,
    val bio: String?,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(user: User) = UserResponse(
            id = user.id,
            email = user.email,
            nickname = user.nickname,
            role = user.role,
            profileImageUrl = user.profileImageUrl,
            bio = user.bio,
            createdAt = user.createdAt
        )
    }
}
