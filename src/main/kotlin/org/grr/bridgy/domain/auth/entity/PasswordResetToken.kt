package org.grr.bridgy.domain.auth.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "password_reset_tokens", indexes = [
    Index(name = "idx_prt_email", columnList = "email")
])
class PasswordResetToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val email: String,

    @Column(nullable = false, length = 6)
    val code: String,

    @Column(nullable = false)
    val expiryDate: LocalDateTime
)