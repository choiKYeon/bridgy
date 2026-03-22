package org.grr.bridgy.kakao.dto

/**
 * 카카오톡 채널 웹훅 요청 DTO (간소화 버전)
 * 실제 카카오 API 스펙에 맞춰 추후 확장 필요
 */
data class KakaoWebhookRequest(
    val userRequest: KakaoUserRequest
)

data class KakaoUserRequest(
    val user: KakaoUser,
    val utterance: String,       // 사용자가 보낸 메시지
    val callbackUrl: String? = null
)

data class KakaoUser(
    val id: String,              // 카카오 사용자 고유 ID
    val properties: Map<String, String>? = null
)

/**
 * 카카오톡 채널 웹훅 응답 DTO (스킬 응답 포맷)
 */
data class KakaoSkillResponse(
    val version: String = "2.0",
    val template: KakaoTemplate
)

data class KakaoTemplate(
    val outputs: List<KakaoOutput>
)

data class KakaoOutput(
    val simpleText: KakaoSimpleText? = null
)

data class KakaoSimpleText(
    val text: String
)

// 편의 팩토리
fun kakaoTextResponse(text: String): KakaoSkillResponse {
    return KakaoSkillResponse(
        template = KakaoTemplate(
            outputs = listOf(
                KakaoOutput(simpleText = KakaoSimpleText(text = text))
            )
        )
    )
}
