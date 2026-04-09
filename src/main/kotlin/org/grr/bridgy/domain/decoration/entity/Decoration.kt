package org.grr.bridgy.domain.decoration.entity

import jakarta.persistence.*
import org.grr.bridgy.common.BaseTime
import org.grr.bridgy.common.enums.SyncableEnum

@Entity
@Table(name = "decorations")
class Decoration(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: DecorationType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val tier: DecorationTier = DecorationTier.BASIC,

    @Column(length = 500)
    val description: String? = null,

    @Column(nullable = false)
    val imageUrl: String, // 테두리/뱃지 이미지 URL

    @Column(nullable = false)
    val isDefault: Boolean = false // 기본 제공 여부
) : BaseTime()

enum class DecorationType(override val description: String) : SyncableEnum {
    BORDER("프로필 테두리"),
    BADGE("뱃지")
}

enum class DecorationTier(override val description: String) : SyncableEnum {
    BASIC("무료 기본 제공"),
    PREMIUM("향후 프리미엄 확장용")
}
