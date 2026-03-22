package org.grr.bridgy.domain.customer.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 카카오톡으로 문의한 고객 (회원가입 불필요)
 * 카카오 유저 ID로 자동 식별/생성됨
 */
@Entity
@Table(name = "kakao_users")
class Customer(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "kakao_user_id", nullable = false, unique = true, length = 100)
    val kakaoUserId: String,

    @Column(length = 50)
    var nickname: String? = null,

    @Column(name = "first_contact_at", nullable = false, updatable = false)
    val firstContactAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "last_contact_at")
    var lastContactAt: LocalDateTime = LocalDateTime.now()
) {
    fun touch() {
        this.lastContactAt = LocalDateTime.now()
    }
}
