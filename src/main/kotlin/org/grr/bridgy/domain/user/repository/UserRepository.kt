package org.grr.bridgy.domain.user.repository

import org.grr.bridgy.domain.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): Optional<User>
    fun findByNickname(nickname: String): Optional<User>
    fun existsByEmail(email: String): Boolean
    fun existsByNickname(nickname: String): Boolean
}
