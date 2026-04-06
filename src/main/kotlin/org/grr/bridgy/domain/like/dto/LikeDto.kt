package org.grr.bridgy.domain.like.dto

data class LikeRequest(
    val petId: Long,
    val userId: Long
)

data class LikeResponse(
    val petId: Long,
    val likeCount: Long,
    val isLiked: Boolean
)
