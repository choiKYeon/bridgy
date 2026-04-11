package org.grr.bridgy.domain.auth.service

import org.grr.bridgy.common.exception.CustomException
import org.grr.bridgy.common.jwt.JwtProvider
import org.grr.bridgy.domain.auth.dto.*
import org.grr.bridgy.domain.auth.entity.PasswordResetToken
import org.grr.bridgy.domain.auth.entity.RefreshToken
import org.grr.bridgy.domain.auth.repository.PasswordResetTokenRepository
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
    private val passwordResetTokenRepository: PasswordResetTokenRepository,
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

    // 아이디(이메일) 찾기 - 닉네임으로 조회 후 마스킹 반환
    fun findId(request: FindIdRequest): FindIdResponse {
        val user = userRepository.findByNickname(request.nickname)
            .orElseThrow { CustomException("해당 닉네임의 사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND) }
        return FindIdResponse(maskedEmail = maskEmail(user.email))
    }

    // 비밀번호 찾기 - 이메일 확인 후 재설정 코드 발급
    @Transactional
    fun requestPasswordReset(request: FindPasswordRequest): FindPasswordResponse {
        if (!userRepository.existsByEmail(request.email)) {
            throw CustomException("등록되지 않은 이메일입니다.", HttpStatus.NOT_FOUND)
        }

        passwordResetTokenRepository.deleteByEmail(request.email)

        val code = (100000..999999).random().toString()
        val expiryDate = LocalDateTime.now().plusMinutes(10)

        passwordResetTokenRepository.save(
            PasswordResetToken(email = request.email, code = code, expiryDate = expiryDate)
        )

        return FindPasswordResponse(resetCode = code)
    }

    // 비밀번호 재설정 - 코드 검증 후 비밀번호 변경
    @Transactional
    fun resetPassword(request: ResetPasswordRequest) {
        val resetToken = passwordResetTokenRepository
            .findByEmailAndCode(request.email, request.resetCode)
            .orElseThrow { CustomException("인증 코드가 올바르지 않습니다.", HttpStatus.BAD_REQUEST) }

        if (resetToken.expiryDate.isBefore(LocalDateTime.now())) {
            passwordResetTokenRepository.delete(resetToken)
            throw CustomException("인증 코드가 만료되었습니다. 다시 요청해주세요.", HttpStatus.BAD_REQUEST)
        }

        val user = userRepository.findByEmail(request.email)
            .orElseThrow { CustomException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND) }

        user.password = passwordEncoder.encode(request.newPassword)
        passwordResetTokenRepository.delete(resetToken)
    }

    private fun maskEmail(email: String): String {
        val (local, domain) = email.split("@")
        val masked = if (local.length <= 2) {
            local.first() + "*".repeat(local.length - 1)
        } else {
            local.take(2) + "*".repeat(local.length - 2)
        }
        return "$masked@$domain"
    }

    private fun findUserIdByToken(token: String): Long? {
        refreshTokenRedisRepository.orElse(null)?.findUserIdByToken(token)?.let { return it }

        val dbToken = refreshTokenRepository.findByToken(token).orElse(null) ?: return null
        if (dbToken.expiryDate.isBefore(LocalDateTime.now())) return null

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