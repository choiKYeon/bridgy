package org.grr.bridgy.domain.auth.service

import org.grr.bridgy.common.exception.CustomException
import org.grr.bridgy.common.jwt.JwtProvider
import org.grr.bridgy.domain.auth.dto.LoginRequest
import org.grr.bridgy.domain.auth.dto.TokenRefreshRequest
import org.grr.bridgy.domain.auth.dto.TokenResponse
import org.grr.bridgy.domain.auth.entity.RefreshToken
import org.grr.bridgy.domain.auth.repository.RefreshTokenRepository
import org.grr.bridgy.domain.user.repository.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtProvider: JwtProvider,
    private val passwordEncoder: PasswordEncoder
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
        val refreshToken = refreshTokenRepository.findByToken(request.refreshToken)
            .orElseThrow { CustomException("유효하지 않은 리프레시 토큰입니다.", HttpStatus.UNAUTHORIZED) }

        if (refreshToken.expiryDate.isBefore(LocalDateTime.now())) {
            throw CustomException("만료된 리프레시 토큰입니다. 다시 로그인해주세요.", HttpStatus.UNAUTHORIZED)
        }

        if (!jwtProvider.validateToken(request.refreshToken)) {
            throw CustomException("유효하지 않은 리프레시 토큰입니다.", HttpStatus.UNAUTHORIZED)
        }

        val user = userRepository.findById(refreshToken.userId)
            .orElseThrow { CustomException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND) }

        val newAccessToken = jwtProvider.createAccessToken(user.id, user.email, user.role.name)
        val newRefreshToken = jwtProvider.createRefreshToken(user.id, user.email, user.role.name)

        saveRefreshToken(user.id, newRefreshToken)

        return TokenResponse(accessToken = newAccessToken, refreshToken = newRefreshToken)
    }

    @Transactional
    fun logout(userId: Long) {
        refreshTokenRepository.deleteByUserId(userId)
    }

    private fun saveRefreshToken(userId: Long, token: String) {
        val expiryDate = LocalDateTime.now().plusSeconds(jwtProvider.getRefreshTokenExpiration() / 1000)

        val existing = refreshTokenRepository.findByUserId(userId)
        if (existing.isPresent) {
            existing.get().token = token
        } else {
            refreshTokenRepository.save(
                RefreshToken(
                    userId = userId,
                    token = token,
                    expiryDate = expiryDate
                )
            )
        }
    }
}
