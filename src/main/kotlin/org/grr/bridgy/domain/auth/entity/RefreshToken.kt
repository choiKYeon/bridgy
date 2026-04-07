package org.grr.bridgy.domain.auth.entity

import jakarta.persistence.*
import org.grr.bridgy.domain.common.BaseTime

@Entity
@Table(name = "refresh_tokens")
class RefreshToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val userId: Long,

    @Column(nullable = false, length = 512)
    var token: String,

    @Column(nullable = false)
    val expiryDate: java.time.LocalDateTime
) : BaseTime()
