package org.grr.bridgy.domain.user.service

import org.grr.bridgy.domain.user.dto.SignUpRequest
import org.grr.bridgy.domain.user.dto.UpdateUserRequest
import org.grr.bridgy.domain.user.dto.UserResponse
import org.grr.bridgy.domain.user.entity.User
import org.grr.bridgy.domain.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository
) {

    @Transactional
    fun signUp(request: SignUpRequest): UserResponse {
        require(!userRepository.existsByEmail(request.email)) { "이미 사용 중인 이메일입니다." }
        require(!userRepository.existsByNickname(request.nickname)) { "이미 사용 중인 닉네임입니다." }

        val user = User(
            email = request.email,
            password = request.password, // TODO: 비밀번호 암호화 적용
            nickname = request.nickname,
            bio = request.bio
        )
        return UserResponse.from(userRepository.save(user))
    }

    fun getUserById(userId: Long): UserResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("사용자를 찾을 수 없습니다. id=$userId") }
        return UserResponse.from(user)
    }

    fun getUserByEmail(email: String): UserResponse {
        val user = userRepository.findByEmail(email)
            .orElseThrow { IllegalArgumentException("사용자를 찾을 수 없습니다. email=$email") }
        return UserResponse.from(user)
    }

    @Transactional
    fun updateUser(userId: Long, request: UpdateUserRequest): UserResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("사용자를 찾을 수 없습니다. id=$userId") }

        request.nickname?.let {
            if (it != user.nickname && userRepository.existsByNickname(it)) {
                throw IllegalArgumentException("이미 사용 중인 닉네임입니다.")
            }
            user.nickname = it
        }
        request.profileImageUrl?.let { user.profileImageUrl = it }
        request.bio?.let { user.bio = it }
        user.updatedAt = LocalDateTime.now()

        return UserResponse.from(userRepository.save(user))
    }

    @Transactional
    fun deleteUser(userId: Long) {
        require(userRepository.existsById(userId)) { "사용자를 찾을 수 없습니다. id=$userId" }
        userRepository.deleteById(userId)
    }
}
