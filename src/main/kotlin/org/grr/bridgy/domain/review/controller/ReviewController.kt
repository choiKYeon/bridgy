package org.grr.bridgy.domain.review.controller

import org.grr.bridgy.domain.review.dto.ReviewCreateRequest
import org.grr.bridgy.domain.review.dto.ReviewResponse
import org.grr.bridgy.domain.review.service.ReviewService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/reviews")
class ReviewController(
    private val reviewService: ReviewService
) {
    @GetMapping("/store/{storeId}")
    fun getReviewsByStore(@PathVariable storeId: Long): ResponseEntity<List<ReviewResponse>> {
        return ResponseEntity.ok(reviewService.getReviewsByStore(storeId))
    }

    @PostMapping
    fun createReview(@RequestBody request: ReviewCreateRequest): ResponseEntity<ReviewResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.createReview(request))
    }

    @PostMapping("/{id}/owner-reply")
    fun addOwnerReply(
        @PathVariable id: Long,
        @RequestBody body: Map<String, String>
    ): ResponseEntity<ReviewResponse> {
        val reply = body["reply"] ?: throw IllegalArgumentException("reply is required")
        return ResponseEntity.ok(reviewService.addOwnerReply(id, reply))
    }
}
