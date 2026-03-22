package org.grr.bridgy.kakao.service

import org.grr.bridgy.ai.service.AiService
import org.grr.bridgy.domain.customer.entity.Customer
import org.grr.bridgy.domain.customer.repository.CustomerRepository
import org.grr.bridgy.domain.store.repository.StoreRepository
import org.grr.bridgy.kakao.dto.KakaoSkillResponse
import org.grr.bridgy.kakao.dto.KakaoWebhookRequest
import org.grr.bridgy.kakao.dto.kakaoTextResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class KakaoChannelService(
    private val customerRepository: CustomerRepository,
    private val storeRepository: StoreRepository,
    private val aiService: AiService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 카카오톡 웹훅 메시지 처리
     * 고객 메시지를 받아 AI 응답을 생성하여 반환
     */
    fun handleMessage(storeId: Long, request: KakaoWebhookRequest): KakaoSkillResponse {
        val kakaoUserId = request.userRequest.user.id
        val message = request.userRequest.utterance

        log.info("[카카오] 메시지 수신 - storeId: $storeId, user: $kakaoUserId, message: $message")

        // 고객 조회 또는 생성
        val customer = customerRepository.findByKakaoUserId(kakaoUserId)
            ?: customerRepository.save(Customer(kakaoUserId = kakaoUserId))

        // 매장 조회
        val store = storeRepository.findById(storeId).orElse(null)
            ?: return kakaoTextResponse("죄송합니다. 매장 정보를 찾을 수 없습니다.")

        // AI 응답 생성
        val reply = aiService.generateCustomerReply(
            storeName = store.name,
            storeDescription = store.description,
            customerMessage = message
        )

        log.info("[카카오] 응답 전송 - storeId: $storeId, reply: $reply")
        return kakaoTextResponse(reply)
    }
}
