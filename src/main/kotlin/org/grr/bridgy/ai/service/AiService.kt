package org.grr.bridgy.ai.service

import org.grr.bridgy.domain.chat.entity.MessageType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalTime

/**
 * AI 응답 생성 서비스
 * - 고객 메시지 자동 응답
 * - 리뷰 자동 답글
 * - 메시지 의도 분류 (일반/예약/영업정보)
 * - 예약 정보 파싱 (자연어 → 구조화 데이터)
 */
@Service
class AiService(
    @Value("\${ai.provider}") private val provider: String
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // ── 메시지 의도 분류 ──

    /**
     * 고객 메시지의 의도를 분류한다
     */
    fun classifyIntent(message: String): MessageType {
        return when {
            isReservationIntent(message) -> MessageType.RESERVATION
            isBusinessInfoIntent(message) -> MessageType.BUSINESS_INFO
            message.contains("리뷰") || message.contains("후기") -> MessageType.REVIEW
            else -> MessageType.GENERAL
        }
    }

    private fun isReservationIntent(message: String): Boolean {
        val keywords = listOf("예약", "예약할", "예약하고", "자리", "테이블", "몇명", "몇 명", "인원")
        return keywords.any { message.contains(it) }
    }

    private fun isBusinessInfoIntent(message: String): Boolean {
        val keywords = listOf("영업시간", "몇시", "언제", "메뉴", "가격", "주차", "위치", "주소", "휴무", "쉬는날")
        return keywords.any { message.contains(it) }
    }

    // ── 예약 정보 파싱 ──

    /**
     * 자연어 메시지에서 예약 정보를 추출한다
     * 예: "내일 저녁 6시에 2명 예약할게요" → ReservationParseResult
     */
    fun parseReservationInfo(message: String): ReservationParseResult {
        val date = parseDate(message)
        val time = parseTime(message)
        val partySize = parsePartySize(message)

        val missingFields = mutableListOf<String>()
        if (date == null) missingFields.add("날짜")
        if (time == null) missingFields.add("시간")
        if (partySize == null) missingFields.add("인원수")

        return ReservationParseResult(
            date = date,
            time = time,
            partySize = partySize,
            isComplete = missingFields.isEmpty(),
            missingFields = missingFields
        )
    }

    private fun parseDate(message: String): LocalDate? {
        val today = LocalDate.now()
        return when {
            message.contains("오늘") -> today
            message.contains("내일") -> today.plusDays(1)
            message.contains("모레") || message.contains("내일모레") -> today.plusDays(2)
            else -> {
                // "4월 5일", "4/5" 등 패턴 매칭
                val dateRegex = Regex("""(\d{1,2})[월/][\s]?(\d{1,2})일?""")
                dateRegex.find(message)?.let {
                    val month = it.groupValues[1].toInt()
                    val day = it.groupValues[2].toInt()
                    try {
                        LocalDate.of(today.year, month, day).let { d ->
                            if (d.isBefore(today)) d.plusYears(1) else d
                        }
                    } catch (e: Exception) { null }
                }
            }
        }
    }

    private fun parseTime(message: String): LocalTime? {
        // "저녁 6시", "오후 3시 30분", "18시", "6시반"
        val timeRegex = Regex("""(\d{1,2})시[\s]?(반|(\d{1,2})분)?""")
        val match = timeRegex.find(message) ?: return null

        var hour = match.groupValues[1].toInt()
        val minutes = when {
            match.groupValues[2] == "반" -> 30
            match.groupValues[3].isNotEmpty() -> match.groupValues[3].toInt()
            else -> 0
        }

        // 오후/저녁 보정
        if (hour in 1..11 && (message.contains("오후") || message.contains("저녁") || message.contains("밤"))) {
            hour += 12
        }

        return try { LocalTime.of(hour, minutes) } catch (e: Exception) { null }
    }

    private fun parsePartySize(message: String): Int? {
        val sizeRegex = Regex("""(\d{1,2})\s*(명|인|분|사람)""")
        return sizeRegex.find(message)?.groupValues?.get(1)?.toIntOrNull()
    }

    // ── 고객 메시지 응답 생성 ──

    fun generateCustomerReply(
        storeName: String,
        storeDescription: String?,
        customerMessage: String
    ): String {
        log.info("[AI] 고객 응답 생성 요청 - 매장: $storeName, 메시지: $customerMessage")

        if (provider == "mock") {
            return generateMockCustomerReply(storeName, customerMessage)
        }

        // TODO: 실제 AI API 연동
        return generateMockCustomerReply(storeName, customerMessage)
    }

    /**
     * 예약 확인 응답 생성
     */
    fun generateReservationConfirmReply(
        storeName: String,
        parseResult: ReservationParseResult
    ): String {
        if (parseResult.isComplete) {
            return "안녕하세요, ${storeName}입니다! " +
                    "${parseResult.date!!.monthValue}월 ${parseResult.date.dayOfMonth}일 " +
                    "${parseResult.time!!.hour}시 ${if (parseResult.time.minute > 0) "${parseResult.time.minute}분" else ""} " +
                    "${parseResult.partySize}명 예약 접수되었습니다. " +
                    "확인 후 안내드리겠습니다!"
        }

        val missing = parseResult.missingFields.joinToString(", ")
        return "안녕하세요, ${storeName}입니다! 예약을 도와드릴게요. " +
                "${missing}을(를) 알려주시겠어요? " +
                "(예: 내일 저녁 6시 2명)"
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

        return generateMockReviewReply(storeName, rating)
    }

    // ── Mock 구현 ──

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

/**
 * 예약 정보 파싱 결과
 */
data class ReservationParseResult(
    val date: LocalDate?,
    val time: LocalTime?,
    val partySize: Int?,
    val isComplete: Boolean,
    val missingFields: List<String>
)
