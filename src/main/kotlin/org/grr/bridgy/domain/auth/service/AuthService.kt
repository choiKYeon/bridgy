package org.grr.bridgy.domain.auth.service

import org.grr.bridgy.common.exception.CustomException
import org.grr.bridgy.common.jwt.JwtProvider
import org.grr.bridgy.domain.auth.dto.LoginRequest
import org.grr.bridgy.domain.auth.dto.TokenRefreshRequest
import org.grr.bridgy.domain.auth.dto.TokenResponse
import org.grr.bridgy.domain.auth.entity.RefreshToken
import org.grr.bridgy.domain.auth.repository.RefreshTokenRedisRepository
import org.grr.bridgy.domain.auth.repository.RefreshTokenRepository
import org.grr.bridgy.domain.user.repository.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.Optional

@Service
@Transactional(readOnly = true)
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtProvider: JwtProvider,
    private val passwordEncoder: PasswordEncoder,
    private val refreshTokenRedisRepository: Optional<RefreshTokenRedisRepository>
) {

    @Transactional
    fun login(request: LoginRequest): TokenResponse {
        val user = userRepository.findByEmail(request.email)
            .orElseThrow { CustomException("이메일 또는 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED) }

        if (!passwordEncoder.matches(request.password, user.password)) {
            throw CustomException("이메일 또는 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED)
        }

        val accessToken = jwtProvider.createAccessToken(user.id, user.email, user.role.name)
        val refreshToken = jwtProvider.createRefreshToken(user.id, user.email, user.role.name)

        saveRefreshToken(user.id, refreshToken)

        return TokenResponse(accessToken = accessToken, refreshToken = refreshToken)
    }

    @Transactional
    fun refresh(request: TokenRefreshRequest): TokenResponse {
        if (!jwtProvider.validateToken(request.refreshToken)) {
            throw CustomException("유효하지 않은 리프레시 토큰입니다.", HttpStatus.UNAUTHORIZED)
        }

        val userId = findUserIdByToken(request.refreshToken)
            ?: throw CustomException("만료되었거나 유효하지 않은 리프레시 토큰입니다. 다시 로그인해주세요.", HttpStatus.UNAUTHORIZED)

        val user = userRepository.findById(userId)
            .orElseThrow { CustomException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND) }

        val newAccessToken = jwtProvider.createAccessToken(user.id, user.email, user.role.name)
        val newRefreshToken = jwtProvider.createRefreshToken(user.id, user.email, user.role.name)

        saveRefreshToken(user.id, newRefreshToken)

        return TokenResponse(accessToken = newAccessToken, refreshToken = newRefreshToken)
    }

    @Transactional
    fun logout(userId: Long) {
        refreshTokenRedisRepository.ifPresent { it.deleteByUserId(userId) }
        refreshTokenRepository.deleteByUserId(userId)
    }

    // Redis 우선 조회 → miss 시 DB 폴백
    private fun findUserIdByToken(token: String): Long? {
        refreshTokenRedisRepository.orElse(null)?.findUserIdByToken(token)?.let { return it }

        val dbToken = refreshTokenRepository.findByToken(token).orElse(null) ?: return null
        if (dbToken.expiryDate.isBefore(LocalDateTime.now())) return null

        // Redis 복구 - 남은 만료 시간으로 재저장
        val remainingMillis = java.time.Duration.between(LocalDateTime.now(), dbToken.expiryDate).toMillis()
        if (remainingMillis > 0) {
            refreshTokenRedisRepository.ifPresent { it.save(dbToken.userId, token, remainingMillis) }
        }

        return dbToken.userId
    }

    private fun saveRefreshToken(userId: Long, token: String) {
        val ttlMillis = jwtProvider.getRefreshTokenExpiration()
        val expiryDate = LocalDateTime.now().plusSeconds(ttlMillis / 1000)

        refreshTokenRedisRepository.ifPresent { it.save(userId, token, ttlMillis) }

        val existing = refreshTokenRepository.findByUserId(userId)
        if (existing.isPresent) {
            existing.get().token = token
            existing.get().expiryDate = expiryDate
        } else {
            refreshTokenRepository.save(RefreshToken(userId = userId, token = token, expiryDate = expiryDate))
        }
    }
}