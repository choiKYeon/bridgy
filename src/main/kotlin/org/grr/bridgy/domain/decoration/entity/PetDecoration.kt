package org.grr.bridgy.domain.decoration.entity

import jakarta.persistence.*
import org.grr.bridgy.common.BaseTime
import org.grr.bridgy.domain.pet.entity.Pet

@Entity
@Table(
    name = "pet_decorations",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["pet_id", "decoration_type"])
    ]
)
class PetDecoration(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    val pet: Pet,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decoration_id", nullable = false)
    val decoration: Decoration,

    @Enumerated(EnumType.STRING)
    @Column(name = "decoration_type", nullable = false)
    val decorationType: DecorationType // 펫당 테두리 1개, 뱃지 1개만 장착 가능
) : BaseTime()
