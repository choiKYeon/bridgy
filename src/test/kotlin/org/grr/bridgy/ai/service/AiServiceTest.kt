package org.grr.bridgy.ai.service

import org.grr.bridgy.domain.chat.entity.MessageType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("AiService 단위 테스트")
class AiServiceTest {

    private val aiService = AiService(provider = "mock")

    // ── 메시지 의도 분류 ──

    @Nested
    @DisplayName("메시지 의도 분류 (classifyIntent)")
    inner class ClassifyIntent {

        @Test
        @DisplayName("예약 관련 키워드가 포함되면 RESERVATION으로 분류한다")
        fun `should classify reservation keywords`() {
            val reservationMessages = listOf(
                "예약하고 싶어요",
                "내일 2명 예약할게요",
                "자리 있나요?",
                "테이블 예약",
                "4명 가능한가요?"
            )
            reservationMessages.forEach { msg ->
                assertEquals(MessageType.RESERVATION, aiService.classifyIntent(msg),
                    "'$msg'는 RESERVATION으로 분류되어야 합니다")
            }
        }

        @Test
        @DisplayName("영업정보 관련 키워드가 포함되면 BUSINESS_INFO로 분류한다")
        fun `should classify business info keywords`() {
            val infoMessages = listOf(
                "영업시간이 어떻게 되나요?",
                "몇시까지 하나요?",
                "메뉴 알려주세요",
                "가격이 어떻게 되나요?",
                "주차 되나요?",
                "위치가 어디에요?",
                "주소 알려주세요",
                "쉬는날이 언제에요?"
            )
            infoMessages.forEach { msg ->
                assertEquals(MessageType.BUSINESS_INFO, aiService.classifyIntent(msg),
                    "'$msg'는 BUSINESS_INFO로 분류되어야 합니다")
            }
        }

        @Test
        @DisplayName("리뷰 관련 키워드가 포함되면 REVIEW로 분류한다")
        fun `should classify review keywords`() {
            assertEquals(MessageType.REVIEW, aiService.classifyIntent("리뷰 남길게요"))
            assertEquals(MessageType.REVIEW, aiService.classifyIntent("후기 작성"))
        }

        @Test
        @DisplayName("일반 메시지는 GENERAL로 분류한다")
        fun `should classify general messages`() {
            val generalMessages = listOf("안녕하세요", "감사합니다", "좋아요")
            generalMessages.forEach { msg ->
                assertEquals(MessageType.GENERAL, aiService.classifyIntent(msg),
                    "'$msg'는 GENERAL로 분류되어야 합니다")
            }
        }
    }

    // ── 예약 정보 파싱 ──

    @Nested
    @DisplayName("예약 정보 파싱 (parseReservationInfo)")
    inner class ParseReservationInfo {

        @Test
        @DisplayName("'내일 저녁 6시 2명 예약'에서 날짜/시간/인원을 모두 추출한다")
        fun `should parse complete reservation info`() {
            val result = aiService.parseReservationInfo("내일 저녁 6시 2명 예약할게요")

            assertTrue(result.isComplete)
            assertEquals(LocalDate.now().plusDays(1), result.date)
            assertEquals(18, result.time?.hour)  // 저녁 6시 → 18시
            assertEquals(2, result.partySize)
            assertTrue(result.missingFields.isEmpty())
        }

        @Test
        @DisplayName("'오늘'은 오늘 날짜로 파싱한다")
        fun `should parse today`() {
            val result = aiService.parseReservationInfo("오늘 7시 3명 예약")
            assertEquals(LocalDate.now(), result.date)
        }

        @Test
        @DisplayName("'모레'는 모레 날짜로 파싱한다")
        fun `should parse day after tomorrow`() {
            val result = aiService.parseReservationInfo("모레 12시 1명")
            assertEquals(LocalDate.now().plusDays(2), result.date)
        }

        @Test
        @DisplayName("'4월 5일' 형식의 날짜를 파싱한다")
        fun `should parse month day format`() {
            val result = aiService.parseReservationInfo("4월 5일 6시 2명 예약")
            assertNotNull(result.date)
            assertEquals(4, result.date?.monthValue)
            assertEquals(5, result.date?.dayOfMonth)
        }

        @Test
        @DisplayName("'6시반'을 6시 30분으로 파싱한다")
        fun `should parse half hour`() {
            val result = aiService.parseReservationInfo("내일 6시반 2명")
            assertNotNull(result.time)
            assertEquals(30, result.time?.minute)
        }

        @Test
        @DisplayName("'오후 3시 30분'을 15시 30분으로 파싱한다")
        fun `should parse PM time with minutes`() {
            val result = aiService.parseReservationInfo("내일 오후 3시 30분 4명")
            assertNotNull(result.time)
            assertEquals(15, result.time?.hour)
            assertEquals(30, result.time?.minute)
        }

        @Test
        @DisplayName("정보가 부족하면 누락 필드를 알려준다")
        fun `should identify missing fields`() {
            val result = aiService.parseReservationInfo("예약하고 싶어요")

            assertFalse(result.isComplete)
            assertTrue(result.missingFields.contains("날짜"))
            assertTrue(result.missingFields.contains("시간"))
            assertTrue(result.missingFields.contains("인원수"))
        }

        @Test
        @DisplayName("날짜만 있고 시간/인원이 없으면 해당 필드를 누락으로 표시한다")
        fun `should identify partially missing fields`() {
            val result = aiService.parseReservationInfo("내일 예약")

            assertFalse(result.isComplete)
            assertNotNull(result.date)
            assertTrue(result.missingFields.contains("시간"))
            assertTrue(result.missingFields.contains("인원수"))
            assertFalse(result.missingFields.contains("날짜"))
        }
    }

    // ── 예약 확인 응답 생성 ──

    @Nested
    @DisplayName("예약 확인 응답 생성 (generateReservationConfirmReply)")
    inner class GenerateReservationConfirmReply {

        @Test
        @DisplayName("정보 완전하면 예약 확인 메시지를 생성한다")
        fun `should generate confirmation when complete`() {
            val parseResult = aiService.parseReservationInfo("내일 저녁 6시 2명 예약")
            val reply = aiService.generateReservationConfirmReply("맛있는 식당", parseResult)

            assertTrue(reply.contains("맛있는 식당"))
            assertTrue(reply.contains("예약 접수"))
            assertTrue(reply.contains("2명"))
        }

        @Test
        @DisplayName("정보 부족하면 누락 정보를 요청하는 메시지를 생성한다")
        fun `should ask for missing info when incomplete`() {
            val parseResult = aiService.parseReservationInfo("예약하고 싶어요")
            val reply = aiService.generateReservationConfirmReply("맛있는 식당", parseResult)

            assertTrue(reply.contains("맛있는 식당"))
            assertTrue(reply.contains("알려주"))
        }
    }

    // ── 고객 메시지 응답 ──

    @Nested
    @DisplayName("고객 메시지 자동 응답 생성")
    inner class GenerateCustomerReply {

        @Test
        @DisplayName("영업시간 문의 시 영업시간 안내 응답을 생성한다")
        fun `should respond to business hours inquiry`() {
            val reply = aiService.generateCustomerReply("맛있는 카페", "아늑한 카페", "영업시간이 어떻게 되나요?")
            assertTrue(reply.contains("맛있는 카페"))
            assertTrue(reply.contains("영업시간"))
        }

        @Test
        @DisplayName("예약 문의 시 예약 안내 응답을 생성한다")
        fun `should respond to reservation inquiry`() {
            val reply = aiService.generateCustomerReply("맛있는 식당", "한식당", "예약하고 싶어요")
            assertTrue(reply.contains("예약"))
        }

        @Test
        @DisplayName("매장 이름이 응답에 항상 포함된다")
        fun `should always include store name`() {
            val messages = listOf("영업시간", "예약", "메뉴", "주차", "안녕하세요")
            messages.forEach { msg ->
                val reply = aiService.generateCustomerReply("테스트매장", null, msg)
                assertTrue(reply.contains("테스트매장"),
                    "'$msg'에 대한 응답에 매장 이름이 포함되어야 합니다")
            }
        }
    }

    // ── 리뷰 자동 답글 ──

    @Nested
    @DisplayName("리뷰 자동 답글 생성")
    inner class GenerateReviewReply {

        @Test
        @DisplayName("높은 평점(4~5점) 리뷰에 감사 답글을 생성한다")
        fun `should generate thankful reply for high ratings`() {
            listOf(4, 5).forEach { rating ->
                val reply = aiService.generateReviewReply("맛있는 카페", "좋았어요", rating)
                assertTrue(reply.contains("감사"), "평점 ${rating}점: $reply")
            }
        }

        @Test
        @DisplayName("중간 평점(3점) 리뷰에 개선 약속 답글을 생성한다")
        fun `should generate improvement reply for medium rating`() {
            val reply = aiService.generateReviewReply("맛있는 카페", "보통이에요", 3)
            assertTrue(reply.contains("개선") || reply.contains("노력"))
        }

        @Test
        @DisplayName("낮은 평점(1~2점) 리뷰에 사과 답글을 생성한다")
        fun `should generate apology reply for low ratings`() {
            listOf(1, 2).forEach { rating ->
                val reply = aiService.generateReviewReply("맛있는 카페", "별로예요", rating)
                assertTrue(reply.contains("죄송") || reply.contains("불편"), "평점 ${rating}점: $reply")
            }
        }
    }
}
