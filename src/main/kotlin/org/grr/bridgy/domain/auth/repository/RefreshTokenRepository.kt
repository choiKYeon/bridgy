package org.grr.bridgy.domain.auth.repository

import org.grr.bridgy.domain.auth.entity.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findByUserId(userId: Long): Optional<RefreshToken>
    fun findByToken(token: String): Optional<RefreshToken>
    fun deleteByUserId(userId: Long)
}