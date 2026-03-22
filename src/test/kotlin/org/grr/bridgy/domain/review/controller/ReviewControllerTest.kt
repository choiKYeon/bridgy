package org.grr.bridgy.domain.review.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.grr.bridgy.domain.review.dto.ReviewCreateRequest
import org.grr.bridgy.domain.review.dto.ReviewResponse
import org.grr.bridgy.domain.review.service.ReviewService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.bean.MockBean
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDateTime

@WebMvcTest(ReviewController::class)
@DisplayName("ReviewController API 테스트")
class ReviewControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockBean
    lateinit var reviewService: ReviewService

    private val objectMapper = ObjectMapper().registerModule(JavaTimeModule())

    private val sampleResponse = ReviewResponse(
        id = 1L, storeId = 10L, customerId = 20L,
        rating = 5, content = "정말 맛있어요!",
        aiReply = "소중한 리뷰 감사합니다!",
        ownerReply = null,
        createdAt = LocalDateTime.of(2026, 3, 22, 10, 0),
        repliedAt = LocalDateTime.of(2026, 3, 22, 10, 1)
    )

    @Nested
    @DisplayName("GET /api/reviews/store/{storeId}")
    inner class GetReviewsByStore {

        @Test
        @WithMockUser
        @DisplayName("매장별 리뷰 목록을 조회한다")
        fun `should return reviews for store`() {
            // given
            whenever(reviewService.getReviewsByStore(10L))
                .thenReturn(listOf(sampleResponse))

            // when & then
            mockMvc.perform(get("/api/reviews/store/10"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].rating").value(5))
                .andExpect(jsonPath("$[0].content").value("정말 맛있어요!"))
                .andExpect(jsonPath("$[0].aiReply").value("소중한 리뷰 감사합니다!"))
        }

        @Test
        @WithMockUser
        @DisplayName("리뷰가 없는 매장은 빈 배열을 반환한다")
        fun `should return empty array when no reviews`() {
            // given
            whenever(reviewService.getReviewsByStore(99L)).thenReturn(emptyList())

            // when & then
            mockMvc.perform(get("/api/reviews/store/99"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.length()").value(0))
        }
    }

    @Nested
    @DisplayName("POST /api/reviews")
    inner class CreateReview {

        @Test
        @WithMockUser
        @DisplayName("리뷰를 등록하면 201 Created와 AI 자동 답글이 포함된 응답을 반환한다")
        fun `should return 201 with AI reply when review created`() {
            // given
            val request = ReviewCreateRequest(
                storeId = 10L, customerId = 20L,
                rating = 5, content = "최고예요!"
            )
            whenever(reviewService.createReview(any())).thenReturn(
                sampleResponse.copy(content = "최고예요!")
            )

            // when & then
            mockMvc.perform(
                post("/api/reviews")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.content").value("최고예요!"))
                .andExpect(jsonPath("$.aiReply").isNotEmpty)
        }

        @Test
        @WithMockUser
        @DisplayName("낮은 평점 리뷰도 정상적으로 등록된다")
        fun `should create review with low rating`() {
            // given
            val request = ReviewCreateRequest(
                storeId = 10L, customerId = 20L,
                rating = 1, content = "별로였어요"
            )
            whenever(reviewService.createReview(any())).thenReturn(
                ReviewResponse(
                    id = 2L, storeId = 10L, customerId = 20L,
                    rating = 1, content = "별로였어요",
                    aiReply = "소중한 의견 감사합니다. 불편을 드려 죄송합니다.",
                    ownerReply = null,
                    createdAt = LocalDateTime.now(), repliedAt = LocalDateTime.now()
                )
            )

            // when & then
            mockMvc.perform(
                post("/api/reviews")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.rating").value(1))
                .andExpect(jsonPath("$.aiReply").isNotEmpty)
        }
    }

    @Nested
    @DisplayName("POST /api/reviews/{id}/owner-reply")
    inner class AddOwnerReply {

        @Test
        @WithMockUser
        @DisplayName("사장님 답글을 등록하면 200 OK를 반환한다")
        fun `should return 200 when owner reply added`() {
            // given
            whenever(reviewService.addOwnerReply(eq(1L), eq("감사합니다! 또 와주세요~")))
                .thenReturn(sampleResponse.copy(ownerReply = "감사합니다! 또 와주세요~"))

            // when & then
            mockMvc.perform(
                post("/api/reviews/1/owner-reply")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"reply": "감사합니다! 또 와주세요~"}""")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.ownerReply").value("감사합니다! 또 와주세요~"))
        }

        @Test
        @WithMockUser
        @DisplayName("reply 필드 없이 요청하면 에러가 발생한다")
        fun `should return error when reply field is missing`() {
            // when & then
            mockMvc.perform(
                post("/api/reviews/1/owner-reply")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"content": "잘못된 필드"}""")
            )
                .andExpect(status().is5xxServerError)
        }
    }
}
