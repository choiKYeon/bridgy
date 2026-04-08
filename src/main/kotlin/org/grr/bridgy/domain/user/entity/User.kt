package org.grr.bridgy.domain.user.entity

import jakarta.persistence.*
import org.grr.bridgy.common.BaseTime

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

    @Column
    var profileImageUrl: String? = null,

    @Column(length = 500)
    var bio: String? = null
) : BaseTime()
