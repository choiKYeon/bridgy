package org.grr.bridgy.domain.comment.entity

import jakarta.persistence.*
import org.grr.bridgy.common.BaseTime
import org.grr.bridgy.domain.pet.entity.Pet
import org.grr.bridgy.domain.user.entity.User

@Entity
@Table(name = "comments")
class Comment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    val pet: Pet,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(nullable = false, length = 1000)
    var content: String
) : BaseTime()
