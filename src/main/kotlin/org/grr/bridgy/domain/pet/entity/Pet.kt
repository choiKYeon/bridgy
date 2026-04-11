package org.grr.bridgy.domain.pet.entity

import jakarta.persistence.*
import org.grr.bridgy.common.BaseTime
import org.grr.bridgy.domain.user.entity.User

@Entity
@Table(name = "pets", indexes = [
    Index(name = "idx_pet_user_id", columnList = "user_id"),
    Index(name = "idx_pet_species", columnList = "species")
])
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
    var profileImageUrl: String? = null
) : BaseTime()

enum class PetGender {
    MALE, FEMALE, NEUTERED_MALE, SPAYED_FEMALE, UNKNOWN
}
