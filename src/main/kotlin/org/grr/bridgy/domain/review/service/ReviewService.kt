package org.grr.bridgy.domain.review.service

import org.grr.bridgy.ai.service.AiService
import org.grr.bridgy.domain.review.dto.ReviewCreateRequest
import org.grr.bridgy.domain.review.dto.ReviewResponse
import org.grr.bridgy.domain.review.entity.Review
import org.grr.bridgy.domain.review.repository.ReviewRepository
import org.grr.bridgy.domain.store.repository.StoreRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class ReviewService(
    private val reviewRepository: ReviewRepository,
    private val storeRepository: StoreRepository,
    private val aiService: AiService
) {
    fun getReviewsByStore(storeId: Long): List<ReviewResponse> {
        return reviewRepository.findByStoreId(storeId).map { ReviewResponse.from(it) }
    }

    @Transactional
    fun createReview(request: ReviewCreateRequest): ReviewResponse {
        val review = Review(
            storeId = request.storeId,
            customerId = request.customerId,
            rating = request.rating,
            content = request.content
        )
        val saved = reviewRepository.save(review)

        // AI 자동 답글 생성
        val store = storeRepository.findById(request.storeId).orElse(null)
        if (store != null) {
            val aiReply = aiService.generateReviewReply(
                storeName = store.name,
                reviewContent = request.content,
                rating = request.rating
            )
            saved.aiReply = aiReply
            saved.repliedAt = LocalDateTime.now()
            reviewRepository.save(saved)
        }

        return ReviewResponse.from(saved)
    }

    @Transactional
    fun addOwnerReply(reviewId: Long, reply: String): ReviewResponse {
        val review = reviewRepository.findById(reviewId)
            .orElseThrow { NoSuchElementException("리뷰를 찾을 수 없습니다: $reviewId") }
        review.ownerReply = reply
        review.repliedAt = LocalDateTime.now()
        return ReviewResponse.from(reviewRepository.save(review))
    }
}
