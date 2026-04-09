package org.grr.bridgy.domain.user.entity

import jakarta.persistence.*
import org.grr.bridgy.common.BaseTime
import org.grr.bridgy.common.enums.SyncableEnum

@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(nullable = false)
    var password: String,

    @Column(nullable = false, unique = true)
    var nickname: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: UserRole = UserRole.USER,

    @Column
    var profileImageUrl: String? = null,

    @Column(length = 500)
    var bio: String? = null
) : BaseTime()

enum class UserRole(override val description: String) : SyncableEnum {
    USER("일반 사용자"),
    ADMIN("관리자"),
    SUPER_ADMIN("최상위 마스터 관리자")
}
