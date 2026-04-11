package org.grr.bridgy.domain.auth.repository

import org.grr.bridgy.domain.auth.entity.PasswordResetToken
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface PasswordResetTokenRepository : JpaRepository<PasswordResetToken, Long> {
    fun findByEmailAndCode(email: String, code: String): Optional<PasswordResetToken>
    fun deleteByEmail(email: String)
}