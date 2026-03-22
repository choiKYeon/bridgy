package org.grr.bridgy.ai.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("AiService 단위 테스트")
class AiServiceTest {

    private val aiService = AiService(provider = "mock")

    @Nested
    @DisplayName("고객 메시지 자동 응답 생성")
    inner class GenerateCustomerReply {

        @Test
        @DisplayName("영업시간 문의 시 영업시간 안내 응답을 생성한다")
        fun `should respond to business hours inquiry`() {
            // when
            val reply = aiService.generateCustomerReply(
                storeName = "맛있는 카페",
                storeDescription = "아늑한 카페",
                customerMessage = "영업시간이 어떻게 되나요?"
            )

            // then
            assertTrue(reply.contains("맛있는 카페"))
            assertTrue(reply.contains("영업시간"))
        }

        @Test
        @DisplayName("'몇시'가 포함된 메시지에도 영업시간 안내를 한다")
        fun `should respond to time inquiry with 몇시`() {
            // when
            val reply = aiService.generateCustomerReply(
                storeName = "테스트 매장",
                storeDescription = null,
                customerMessage = "몇시까지 하나요?"
            )

            // then
            assertTrue(reply.contains("테스트 매장"))
            assertTrue(reply.contains("영업시간"))
        }

        @Test
        @DisplayName("예약 관련 메시지에 예약 안내 응답을 생성한다")
        fun `should respond to reservation inquiry`() {
            // when
            val reply = aiService.generateCustomerReply(
                storeName = "맛있는 식당",
                storeDescription = "한식당",
                customerMessage = "예약하고 싶어요"
            )

            // then
            assertTrue(reply.contains("맛있는 식당"))
            assertTrue(reply.contains("예약"))
            assertTrue(reply.contains("날짜") || reply.contains("시간") || reply.contains("인원"))
        }

        @Test
        @DisplayName("메뉴/가격 문의에 메뉴 안내 응답을 생성한다")
        fun `should respond to menu inquiry`() {
            // when
            val reply = aiService.generateCustomerReply(
                storeName = "맛있는 카페",
                storeDescription = null,
                customerMessage = "메뉴 좀 알려주세요"
            )

            // then
            assertTrue(reply.contains("메뉴"))
        }

        @Test
        @DisplayName("가격 문의에도 메뉴 안내 응답을 생성한다")
        fun `should respond to price inquiry`() {
            // when
            val reply = aiService.generateCustomerReply(
                storeName = "맛있는 카페",
                storeDescription = null,
                customerMessage = "가격이 어떻게 되나요?"
            )

            // then
            assertTrue(reply.contains("메뉴") || reply.contains("가격"))
        }

        @Test
        @DisplayName("주차 문의에 주차 안내 응답을 생성한다")
        fun `should respond to parking inquiry`() {
            // when
            val reply = aiService.generateCustomerReply(
                storeName = "맛있는 식당",
                storeDescription = null,
                customerMessage = "주차 가능한가요?"
            )

            // then
            assertTrue(reply.contains("주차"))
        }

        @Test
        @DisplayName("일반 문의에는 기본 응답을 생성한다")
        fun `should respond with default reply for general inquiry`() {
            // when
            val reply = aiService.generateCustomerReply(
                storeName = "맛있는 카페",
                storeDescription = null,
                customerMessage = "안녕하세요"
            )

            // then
            assertTrue(reply.contains("맛있는 카페"))
            assertTrue(reply.contains("문의") || reply.contains("감사"))
        }

        @Test
        @DisplayName("매장 이름이 응답에 항상 포함된다")
        fun `should always include store name in reply`() {
            val messages = listOf("영업시간", "예약", "메뉴", "주차", "기타 문의")

            messages.forEach { message ->
                val reply = aiService.generateCustomerReply(
                    storeName = "테스트매장",
                    storeDescription = null,
                    customerMessage = message
                )
                assertTrue(reply.contains("테스트매장"),
                    "메시지 '$message'에 대한 응답에 매장 이름이 포함되어야 합니다: $reply")
            }
        }
    }

    @Nested
    @DisplayName("리뷰 자동 답글 생성")
    inner class GenerateReviewReply {

        @Test
        @DisplayName("높은 평점(4~5점) 리뷰에 감사 답글을 생성한다")
        fun `should generate thankful reply for high ratings`() {
            listOf(4, 5).forEach { rating ->
                val reply = aiService.generateReviewReply(
                    storeName = "맛있는 카페",
                    reviewContent = "정말 좋았어요",
                    rating = rating
                )
                assertTrue(reply.contains("감사"),
                    "평점 ${rating}점 리뷰 답글에 '감사'가 포함되어야 합니다: $reply")
                assertTrue(reply.contains("맛있는 카페"))
            }
        }

        @Test
        @DisplayName("중간 평점(3점) 리뷰에 개선 약속 답글을 생성한다")
        fun `should generate improvement reply for medium rating`() {
            // when
            val reply = aiService.generateReviewReply(
                storeName = "맛있는 카페",
                reviewContent = "보통이에요",
                rating = 3
            )

            // then
            assertTrue(reply.contains("개선") || reply.contains("노력"),
                "3점 리뷰 답글에 개선/노력 관련 내용이 포함되어야 합니다: $reply")
        }

        @Test
        @DisplayName("낮은 평점(1~2점) 리뷰에 사과 답글을 생성한다")
        fun `should generate apology reply for low ratings`() {
            listOf(1, 2).forEach { rating ->
                val reply = aiService.generateReviewReply(
                    storeName = "맛있는 카페",
                    reviewContent = "별로예요",
                    rating = rating
                )
                assertTrue(reply.contains("죄송") || reply.contains("불편"),
                    "평점 ${rating}점 리뷰 답글에 사과 내용이 포함되어야 합니다: $reply")
            }
        }
    }
}
