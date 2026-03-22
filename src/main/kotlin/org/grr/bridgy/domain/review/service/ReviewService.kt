package org.grr.bridgy.domain.review.service

import org.grr.bridgy.ai.service.AiService
import org.grr.bridgy.domain.review.dto.ReviewCreateRequest
import org.grr.bridgy.domain.review.dto.ReviewResponse
import org.grr.bridgy.domain.review.entity.Review
import org.grr.bridgy.domain.review.repository.ReviewRepository
import org.grr.bridgy.domain.store.repository.StoreRepository
import org.grr.bridgy.kafka.event.NotificationEvent
import org.grr.bridgy.kafka.event.NotificationType
import org.grr.bridgy.kafka.event.ReviewEvent
import org.grr.bridgy.kafka.producer.EventProducer
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class ReviewService(
    private val reviewRepository: ReviewRepository,
    private val storeRepository: StoreRepository,
    private val aiService: AiService,
    private val eventProducer: EventProducer
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

        // 리뷰 이벤트 발행
        eventProducer.publishReviewEvent(
            ReviewEvent(
                storeId = request.storeId,
                reviewId = saved.id,
                rating = request.rating,
                content = request.content
            )
        )

        // 사장님 알림 발행
        if (store != null) {
            eventProducer.publishNotification(
                NotificationEvent(
                    storeId = request.storeId,
                    ownerEmail = store.ownerEmail,
                    title = "새로운 리뷰 등록",
                    message = "평점: ${request.rating}점 - ${request.content.take(50)}",
                    notificationType = NotificationType.NEW_REVIEW
                )
            )
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
