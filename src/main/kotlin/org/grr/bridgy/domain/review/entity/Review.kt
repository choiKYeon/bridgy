package org.grr.bridgy.domain.review.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "reviews")
class Review(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "store_id", nullable = false)
    val storeId: Long,

    @Column(name = "customer_id", nullable = false)
    val customerId: Long,

    @Column(nullable = false)
    var rating: Int,

    @Column(nullable = false, length = 2000)
    var content: String,

    @Column(name = "ai_reply", length = 2000)
    var aiReply: String? = null,

    @Column(name = "owner_reply", length = 2000)
    var ownerReply: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "replied_at")
    var repliedAt: LocalDateTime? = null
)
