package org.grr.bridgy.domain.decoration.config

import org.grr.bridgy.domain.decoration.entity.Decoration
import org.grr.bridgy.domain.decoration.entity.DecorationTier
import org.grr.bridgy.domain.decoration.entity.DecorationType
import org.grr.bridgy.domain.decoration.repository.DecorationRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("!test")  // 테스트 환경에서는 비활성화
class DecorationDataInitializer(
    private val decorationRepository: DecorationRepository
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        if (decorationRepository.count() > 0) return // 이미 데이터가 있으면 스킵

        val defaultDecorations = listOf(
            // 기본 테두리
            Decoration(
                name = "기본 테두리",
                type = DecorationType.BORDER,
                tier = DecorationTier.BASIC,
                description = "심플한 기본 테두리",
                imageUrl = "/decorations/borders/default.png",
                isDefault = true
            ),
            Decoration(
                name = "하트 테두리",
                type = DecorationType.BORDER,
                tier = DecorationTier.BASIC,
                description = "사랑스러운 하트 테두리",
                imageUrl = "/decorations/borders/heart.png",
                isDefault = false
            ),
            Decoration(
                name = "발바닥 테두리",
                type = DecorationType.BORDER,
                tier = DecorationTier.BASIC,
                description = "귀여운 발바닥 무늬 테두리",
                imageUrl = "/decorations/borders/paw.png",
                isDefault = false
            ),
            Decoration(
                name = "별 테두리",
                type = DecorationType.BORDER,
                tier = DecorationTier.BASIC,
                description = "반짝이는 별 테두리",
                imageUrl = "/decorations/borders/star.png",
                isDefault = false
            ),
            Decoration(
                name = "무지개 테두리",
                type = DecorationType.BORDER,
                tier = DecorationTier.BASIC,
                description = "알록달록 무지개 테두리",
                imageUrl = "/decorations/borders/rainbow.png",
                isDefault = false
            ),

            // 기본 뱃지
            Decoration(
                name = "새싹 뱃지",
                type = DecorationType.BADGE,
                tier = DecorationTier.BASIC,
                description = "새로운 친구 환영!",
                imageUrl = "/decorations/badges/sprout.png",
                isDefault = true
            ),
            Decoration(
                name = "뼈다귀 뱃지",
                type = DecorationType.BADGE,
                tier = DecorationTier.BASIC,
                description = "강아지 전용 뱃지",
                imageUrl = "/decorations/badges/bone.png",
                isDefault = false
            ),
            Decoration(
                name = "물고기 뱃지",
                type = DecorationType.BADGE,
                tier = DecorationTier.BASIC,
                description = "고양이 전용 뱃지",
                imageUrl = "/decorations/badges/fish.png",
                isDefault = false
            ),
            Decoration(
                name = "발바닥 뱃지",
                type = DecorationType.BADGE,
                tier = DecorationTier.BASIC,
                description = "모든 반려동물을 위한 뱃지",
                imageUrl = "/decorations/badges/paw.png",
                isDefault = false
            ),
            Decoration(
                name = "왕관 뱃지",
                type = DecorationType.BADGE,
                tier = DecorationTier.BASIC,
                description = "우리 집 왕자/공주님",
                imageUrl = "/decorations/badges/crown.png",
                isDefault = false
            )
        )

        decorationRepository.saveAll(defaultDecorations)
    }
}
