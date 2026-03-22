package org.grr.bridgy.ai.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class AiService(
    @Value("\${ai.provider}") private val provider: String
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 고객 메시지에 대한 AI 자동 응답 생성
     */
    fun generateCustomerReply(
        storeName: String,
        storeDescription: String?,
        customerMessage: String
    ): String {
        log.info("[AI] 고객 응답 생성 요청 - 매장: $storeName, 메시지: $customerMessage")

        if (provider == "mock") {
            return generateMockCustomerReply(storeName, customerMessage)
        }

        // TODO: 실제 AI API 연동 (OpenAI, Claude 등)
        return generateMockCustomerReply(storeName, customerMessage)
    }

    /**
     * 리뷰 자동 답글 생성
     */
    fun generateReviewReply(
        storeName: String,
        reviewContent: String,
        rating: Int
    ): String {
        log.info("[AI] 리뷰 답글 생성 요청 - 매장: $storeName, 평점: $rating")

        if (provider == "mock") {
            return generateMockReviewReply(storeName, rating)
        }

        // TODO: 실제 AI API 연동
        return generateMockReviewReply(storeName, rating)
    }

    private fun generateMockCustomerReply(storeName: String, message: String): String {
        return when {
            message.contains("영업시간") || message.contains("몇시") ->
                "안녕하세요, ${storeName}입니다! 영업시간은 매장 정보를 확인해 주세요. 추가 문의사항이 있으시면 말씀해 주세요 😊"
            message.contains("예약") ->
                "안녕하세요, ${storeName}입니다! 예약을 도와드리겠습니다. 원하시는 날짜, 시간, 인원수를 알려주세요!"
            message.contains("메뉴") || message.contains("가격") ->
                "안녕하세요, ${storeName}입니다! 메뉴와 가격 정보는 매장 페이지에서 확인하실 수 있습니다. 궁금한 점이 있으시면 편하게 물어보세요!"
            message.contains("주차") ->
                "안녕하세요, ${storeName}입니다! 주차 관련 안내는 매장에 직접 문의해 주시면 더 정확한 안내를 받으실 수 있습니다."
            else ->
                "안녕하세요, ${storeName}입니다! 문의 감사합니다. 자세한 내용은 매장으로 연락 부탁드립니다. 좋은 하루 되세요! 😊"
        }
    }

    private fun generateMockReviewReply(storeName: String, rating: Int): String {
        return when {
            rating >= 4 ->
                "소중한 리뷰 감사합니다! ${storeName}을(를) 좋게 봐주셔서 정말 감사해요. 더 좋은 서비스로 보답하겠습니다. 또 방문해 주세요! 😊"
            rating == 3 ->
                "리뷰 남겨주셔서 감사합니다. 더 나은 서비스를 위해 노력하겠습니다. 다음 방문 시에는 더 만족하실 수 있도록 개선하겠습니다!"
            else ->
                "소중한 의견 감사합니다. 불편을 드려 죄송합니다. 말씀해 주신 부분을 꼼꼼히 살펴보고 개선하겠습니다. 다시 방문해 주시면 더 나은 경험을 드리겠습니다."
        }
    }
}
