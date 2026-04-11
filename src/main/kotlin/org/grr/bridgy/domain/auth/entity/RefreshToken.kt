package org.grr.bridgy.domain.auth.entity

import jakarta.persistence.*
import org.grr.bridgy.common.BaseTime

@Entity
@Table(name = "refresh_tokens", indexes = [
    Index(name = "idx_refresh_token_user_id", columnList = "user_id"),
    Index(name = "idx_refresh_token_token", columnList = "token")
])
class RefreshToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val userId: Long,

    @Column(nullable = false, length = 512)
    var token: String,

    @Column(nullable = false)
    var expiryDate: java.time.LocalDateTime
) : BaseTime()