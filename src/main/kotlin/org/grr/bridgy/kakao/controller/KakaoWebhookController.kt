package org.grr.bridgy.kakao.controller

import org.grr.bridgy.kakao.dto.KakaoSkillResponse
import org.grr.bridgy.kakao.dto.KakaoWebhookRequest
import org.grr.bridgy.kakao.service.KakaoChannelService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 카카오톡 채널 웹훅 컨트롤러
 * 카카오 i 오픈빌더 스킬 서버로 동작
 */
@RestController
@RequestMapping("/api/kakao")
class KakaoWebhookController(
    private val kakaoChannelService: KakaoChannelService
) {
    /**
     * 카카오톡 채널 메시지 수신 웹훅
     * 카카오 i 오픈빌더에서 스킬 URL로 등록
     */
    @PostMapping("/webhook/{storeId}")
    fun handleWebhook(
        @PathVariable storeId: Long,
        @RequestBody request: KakaoWebhookRequest
    ): ResponseEntity<KakaoSkillResponse> {
        val response = kakaoChannelService.handleMessage(storeId, request)
        return ResponseEntity.ok(response)
    }

    /**
     * 웹훅 헬스체크 (카카오 오픈빌더 스킬 등록 시 사용)
     */
    @GetMapping("/webhook/health")
    fun healthCheck(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf("status" to "ok"))
    }
}
