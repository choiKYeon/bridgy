package org.grr.bridgy.domain.user.service

import org.grr.bridgy.common.exception.CustomException
import org.grr.bridgy.domain.user.dto.SignUpRequest
import org.grr.bridgy.domain.user.dto.UpdateUserRequest
import org.grr.bridgy.domain.user.dto.UserResponse
import org.grr.bridgy.domain.user.entity.User
import org.grr.bridgy.domain.user.repository.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    @Transactional
    fun signUp(request: SignUpRequest): UserResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw CustomException("이미 사용 중인 이메일입니다.", HttpStatus.CONFLICT)
        }
        if (userRepository.existsByNickname(request.nickname)) {
            throw CustomException("이미 사용 중인 닉네임입니다.", HttpStatus.CONFLICT)
        }

        val user = User(
            email = request.email,
            password = passwordEncoder.encode(request.password),
            nickname = request.nickname,
            bio = request.bio
        )
        return UserResponse.from(userRepository.save(user))
    }

    fun getUserById(userId: Long): UserResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { CustomException("사용자를 찾을 수 없습니다. id=$userId", HttpStatus.NOT_FOUND) }
        return UserResponse.from(user)
    }

    fun getUserByEmail(email: String): UserResponse {
        val user = userRepository.findByEmail(email)
            .orElseThrow { CustomException("사용자를 찾을 수 없습니다. email=$email", HttpStatus.NOT_FOUND) }
        return UserResponse.from(user)
    }

    @Transactional
    fun updateUser(userId: Long, request: UpdateUserRequest): UserResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { CustomException("사용자를 찾을 수 없습니다. id=$userId", HttpStatus.NOT_FOUND) }

        request.nickname?.let {
            if (it != user.nickname && userRepository.existsByNickname(it)) {
                throw CustomException("이미 사용 중인 닉네임입니다.", HttpStatus.CONFLICT)
            }
            user.nickname = it
        }
        request.profileImageUrl?.let { user.profileImageUrl = it }
        request.bio?.let { user.bio = it }

        return UserResponse.from(userRepository.save(user))
    }

    @Transactional
    fun deleteUser(userId: Long) {
        if (!userRepository.existsById(userId)) {
            throw CustomException("사용자를 찾을 수 없습니다. id=$userId", HttpStatus.NOT_FOUND)
        }
        userRepository.deleteById(userId)
    }
}
