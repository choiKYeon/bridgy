package org.grr.bridgy.domain.customer.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "customers")
class Customer(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "kakao_user_id", nullable = false, unique = true, length = 100)
    val kakaoUserId: String,

    @Column(length = 50)
    var nickname: String? = null,

    @Column(length = 20)
    var phone: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
