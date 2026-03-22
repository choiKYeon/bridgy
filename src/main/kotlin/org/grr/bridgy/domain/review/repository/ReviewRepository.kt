package org.grr.bridgy.domain.review.repository

import org.grr.bridgy.domain.review.entity.Review
import org.springframework.data.jpa.repository.JpaRepository

interface ReviewRepository : JpaRepository<Review, Long> {
    fun findByStoreId(storeId: Long): List<Review>
    fun findByStoreIdAndAiReplyIsNull(storeId: Long): List<Review>
    fun findByCustomerId(customerId: Long): List<Review>
}
