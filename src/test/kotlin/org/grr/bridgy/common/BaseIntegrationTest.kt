package org.grr.bridgy.common

import com.fasterxml.jackson.databind.ObjectMapper
import org.grr.bridgy.common.jwt.JwtProvider
import org.grr.bridgy.domain.user.entity.User
import org.grr.bridgy.domain.user.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.transaction.annotation.Transactional

/**
 * 통합 테스트 베이스 클래스.
 * - H2 인메모리 DB 사용
 * - 매 테스트마다 트랜잭션 롤백
 * - JWT 토큰 생성 헬퍼 제공 (V1 인증 API 테스트용)
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
abstract class BaseIntegrationTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var jwtProvider: JwtProvider

    @Autowired
    lateinit var userRepository: UserRepository

    /**
     * 테스트용 사용자 생성
     */
    protected fun createTestUser(
        email: String = "test@example.com",
        password: String = "password123",
        nickname: String = "테스트유저"
    ): User {
        return userRepository.save(
            User(
                email = email,
                password = password,
                nickname = nickname,
                bio = "테스트 소개글"
            )
        )
    }

    /**
     * 해당 사용자의 JWT Access Token 생성
     */
    protected fun accessTokenFor(user: User): String {
        return jwtProvider.createAccessToken(user.id, user.email, user.role.name)
    }

    /**
     * V1 (인증 필요) 요청에 Authorization 헤더를 추가하는 확장 함수
     */
    protected fun MockHttpServletRequestBuilder.withAuth(user: User): MockHttpServletRequestBuilder {
        return this.header("Authorization", "Bearer ${accessTokenFor(user)}")
    }

    /**
     * JSON 직렬화 헬퍼
     */
    protected fun toJson(obj: Any): String = objectMapper.writeValueAsString(obj)
}
