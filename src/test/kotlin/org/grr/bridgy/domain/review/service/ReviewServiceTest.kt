package org.grr.bridgy.domain.review.service

import org.grr.bridgy.ai.service.AiService
import org.grr.bridgy.domain.review.dto.ReviewCreateRequest
import org.grr.bridgy.domain.review.entity.Review
import org.grr.bridgy.domain.review.repository.ReviewRepository
import org.grr.bridgy.domain.store.entity.Store
import org.grr.bridgy.domain.store.repository.StoreRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("ReviewService 단위 테스트")
class ReviewServiceTest {

    @Mock
    lateinit var reviewRepository: ReviewRepository

    @Mock
    lateinit var storeRepository: StoreRepository

    @Mock
    lateinit var aiService: AiService

    @InjectMocks
    lateinit var reviewService: ReviewService

    private lateinit var sampleStore: Store
    private lateinit var sampleReview: Review

    @BeforeEach
    fun setUp() {
        sampleStore = Store(
            id = 1L, name = "맛있는 카페", category = "카페",
            address = "서울시 강남구", phone = "02-1234-5678",
            ownerEmail = "owner@example.com"
        )
        sampleReview = Review(
            id = 1L, storeId = 1L, customerId = 10L,
            rating = 5, content = "커피가 정말 맛있어요!",
            aiReply = "소중한 리뷰 감사합니다!"
        )
    }

    @Nested
    @DisplayName("매장별 리뷰 조회")
    inner class GetReviewsByStore {

        @Test
        @DisplayName("매장 ID로 리뷰 목록을 조회한다")
        fun `should return reviews for a store`() {
            // given
            val review2 = Review(
                id = 2L, storeId = 1L, customerId = 20L,
                rating = 3, content = "보통이에요"
            )
            whenever(reviewRepository.findByStoreId(1L))
                .thenReturn(listOf(sampleReview, review2))

            // when
            val result = reviewService.getReviewsByStore(1L)

            // then
            assertEquals(2, result.size)
            assertEquals(5, result[0].rating)
            assertEquals(3, result[1].rating)
        }

        @Test
        @DisplayName("리뷰가 없는 매장은 빈 리스트를 반환한다")
        fun `should return empty list when no reviews`() {
            // given
            whenever(reviewRepository.findByStoreId(99L)).thenReturn(emptyList())

            // when
            val result = reviewService.getReviewsByStore(99L)

            // then
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("리뷰 등록 + AI 자동 답글")
    inner class CreateReview {

        @Test
        @DisplayName("리뷰를 등록하면 AI가 자동으로 답글을 생성한다")
        fun `should create review with AI auto-reply`() {
            // given
            val request = ReviewCreateRequest(
                storeId = 1L, customerId = 10L,
                rating = 5, content = "최고예요!"
            )
            whenever(reviewRepository.save(any<Review>())).thenAnswer { invocation ->
                val saved = invocation.getArgument<Review>(0)
                Review(
                    id = 100L, storeId = saved.storeId, customerId = saved.customerId,
                    rating = saved.rating, content = saved.content,
                    aiReply = saved.aiReply, repliedAt = saved.repliedAt
                )
            }
            whenever(storeRepository.findById(1L)).thenReturn(Optional.of(sampleStore))
            whenever(aiService.generateReviewReply(
                storeName = "맛있는 카페",
                reviewContent = "최고예요!",
                rating = 5
            )).thenReturn("소중한 리뷰 감사합니다! 맛있는 카페을(를) 좋게 봐주셔서 정말 감사해요.")

            // when
            val result = reviewService.createReview(request)

            // then
            assertEquals(100L, result.id)
            assertEquals(5, result.rating)
            assertEquals("최고예요!", result.content)
            assertNotNull(result.aiReply)
            assertTrue(result.aiReply!!.contains("감사"))
            assertNotNull(result.repliedAt)
            verify(aiService).generateReviewReply(
                storeName = "맛있는 카페",
                reviewContent = "최고예요!",
                rating = 5
            )
        }

        @Test
        @DisplayName("매장이 존재하지 않으면 AI 답글 없이 리뷰만 저장된다")
        fun `should save review without AI reply when store not found`() {
            // given
            val request = ReviewCreateRequest(
                storeId = 999L, customerId = 10L,
                rating = 4, content = "좋아요"
            )
            whenever(reviewRepository.save(any<Review>())).thenAnswer { invocation ->
                val saved = invocation.getArgument<Review>(0)
                Review(
                    id = 101L, storeId = saved.storeId, customerId = saved.customerId,
                    rating = saved.rating, content = saved.content,
                    aiReply = saved.aiReply
                )
            }
            whenever(storeRepository.findById(999L)).thenReturn(Optional.empty())

            // when
            val result = reviewService.createReview(request)

            // then
            assertEquals(101L, result.id)
            assertNull(result.aiReply)
            verify(aiService, never()).generateReviewReply(any(), any(), any())
        }

        @Test
        @DisplayName("낮은 평점 리뷰에도 AI 답글이 생성된다")
        fun `should generate AI reply for low rating reviews`() {
            // given
            val request = ReviewCreateRequest(
                storeId = 1L, customerId = 10L,
                rating = 1, content = "별로였어요"
            )
            whenever(reviewRepository.save(any<Review>())).thenAnswer { invocation ->
                val saved = invocation.getArgument<Review>(0)
                Review(
                    id = 102L, storeId = saved.storeId, customerId = saved.customerId,
                    rating = saved.rating, content = saved.content,
                    aiReply = saved.aiReply, repliedAt = saved.repliedAt
                )
            }
            whenever(storeRepository.findById(1L)).thenReturn(Optional.of(sampleStore))
            whenever(aiService.generateReviewReply(
                storeName = "맛있는 카페",
                reviewContent = "별로였어요",
                rating = 1
            )).thenReturn("소중한 의견 감사합니다. 불편을 드려 죄송합니다.")

            // when
            val result = reviewService.createReview(request)

            // then
            assertEquals(1, result.rating)
            assertNotNull(result.aiReply)
            assertTrue(result.aiReply!!.contains("죄송"))
        }
    }

    @Nested
    @DisplayName("사장님 수동 답글")
    inner class AddOwnerReply {

        @Test
        @DisplayName("사장님이 리뷰에 수동 답글을 달 수 있다")
        fun `should add owner reply to review`() {
            // given
            whenever(reviewRepository.findById(1L)).thenReturn(Optional.of(sampleReview))
            whenever(reviewRepository.save(any<Review>())).thenAnswer { it.getArgument<Review>(0) }

            // when
            val result = reviewService.addOwnerReply(1L, "감사합니다! 또 와주세요~")

            // then
            assertEquals("감사합니다! 또 와주세요~", result.ownerReply)
            assertNotNull(result.repliedAt)
        }

        @Test
        @DisplayName("존재하지 않는 리뷰에 답글을 달면 예외가 발생한다")
        fun `should throw exception when review not found`() {
            // given
            whenever(reviewRepository.findById(999L)).thenReturn(Optional.empty())

            // when & then
            val exception = assertThrows(NoSuchElementException::class.java) {
                reviewService.addOwnerReply(999L, "답글")
            }
            assertTrue(exception.message!!.contains("리뷰를 찾을 수 없습니다"))
        }
    }
}
