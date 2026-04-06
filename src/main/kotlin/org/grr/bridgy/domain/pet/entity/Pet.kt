package org.grr.bridgy.domain.pet.entity

import jakarta.persistence.*
import org.grr.bridgy.domain.user.entity.User
import java.time.LocalDateTime

@Entity
@Table(name = "pets")
class Pet(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false)
    var species: String, // 강아지, 고양이, 햄스터, 토끼, 새, 물고기, 파충류, 기타

    @Column
    var breed: String? = null, // 품종

    @Column
    var age: Int? = null,

    @Enumerated(EnumType.STRING)
    @Column
    var gender: PetGender? = null,

    @Column
    var weight: Double? = null, // kg

    @Column(length = 1000)
    var bio: String? = null,

    @Column
    var profileImageUrl: String? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class PetGender {
    MALE, FEMALE, NEUTERED_MALE, SPAYED_FEMALE, UNKNOWN
}
