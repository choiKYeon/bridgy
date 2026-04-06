package org.grr.bridgy.domain.comment.dto

import org.grr.bridgy.domain.comment.entity.Comment
import java.time.LocalDateTime

data class CreateCommentRequest(
    val petId: Long,
    val userId: Long,
    val content: String
)

data class UpdateCommentRequest(
    val content: String
)

data class CommentResponse(
    val id: Long,
    val petId: Long,
    val userId: Long,
    val userNickname: String,
    val userProfileImageUrl: String?,
    val content: String,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(comment: Comment) = CommentResponse(
            id = comment.id,
            petId = comment.pet.id,
            userId = comment.user.id,
            userNickname = comment.user.nickname,
            userProfileImageUrl = comment.user.profileImageUrl,
            content = comment.content,
            createdAt = comment.createdAt
        )
    }
}
