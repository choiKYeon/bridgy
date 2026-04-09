package org.grr.bridgy.common.config

import jakarta.persistence.EntityManager
import org.grr.bridgy.domain.user.entity.User
import org.grr.bridgy.domain.user.entity.UserRole
import org.grr.bridgy.domain.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Profile("!test")
class AdminDataInitializer(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val entityManager: EntityManager
) : CommandLineRunner {

    private val log = LoggerFactory.getLogger(AdminDataInitializer::class.java)

    companion object {
        const val ADMIN_EMAIL = "admin@bridgy.com"
        const val ADMIN_NICKNAME = "bridgy_admin"
        const val ADMIN_DEFAULT_PASSWORD = "bridgy@admin2026!"
    }

    @Transactional
    override fun run(vararg args: String?) {
        if (userRepository.existsByEmail(ADMIN_EMAIL)) {
            log.info("마스터 관리자 계정이 이미 존재합니다. (email: $ADMIN_EMAIL)")
            return
        }

        // PostgreSQL 시퀀스 동기화 (기존 데이터가 있을 때 시퀀스 충돌 방지)
        try {
            entityManager.createNativeQuery(
                "SELECT setval('users_id_seq', (SELECT COALESCE(MAX(id), 0) FROM users))"
            ).singleResult
        } catch (e: Exception) {
            log.warn("시퀀스 동기화 실패 (무시 가능): ${e.message}")
        }

        val admin = User(
            email = ADMIN_EMAIL,
            password = passwordEncoder.encode(ADMIN_DEFAULT_PASSWORD),
            nickname = ADMIN_NICKNAME,
            role = UserRole.SUPER_ADMIN,
            bio = "Bridgy 최상위 관리자 계정"
        )

        userRepository.save(admin)
        log.info("============================================")
        log.info("  마스터 관리자 계정 생성 완료")
        log.info("  Email: $ADMIN_EMAIL")
        log.info("  Password: $ADMIN_DEFAULT_PASSWORD")
        log.info("  Role: SUPER_ADMIN")
        log.info("  운영 환경에서는 반드시 비밀번호를 변경하세요!")
        log.info("============================================")
    }
}
