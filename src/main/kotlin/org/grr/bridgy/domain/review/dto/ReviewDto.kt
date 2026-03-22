package org.grr.bridgy.domain.review.dto

import org.grr.bridgy.domain.review.entity.Review
import java.time.LocalDateTime

data class ReviewCreateRequest(
    val storeId: Long,
    val customerId: Long,
    val rating: Int,
    val content: String
)

data class ReviewResponse(
    val id: Long,
    val storeId: Long,
    val customerId: Long,
    val rating: Int,
    val content: String,
    val aiReply: String?,
    val ownerReply: String?,
    val createdAt: LocalDateTime,
    val repliedAt: LocalDateTime?
) {
    companion object {
        fun from(review: Review) = ReviewResponse(
            id = review.id,
            storeId = review.storeId,
            customerId = review.customerId,
            rating = review.rating,
            content = review.content,
            aiReply = review.aiReply,
            ownerReply = review.ownerReply,
            createdAt = review.createdAt,
            repliedAt = review.repliedAt
        )
    }
}
