package org.grr.bridgy.domain.store.service

import org.grr.bridgy.domain.store.dto.StoreCreateRequest
import org.grr.bridgy.domain.store.dto.StoreUpdateRequest
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
import java.time.LocalTime
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("StoreService 단위 테스트")
class StoreServiceTest {

    @Mock
    lateinit var storeRepository: StoreRepository

    @InjectMocks
    lateinit var storeService: StoreService

    private lateinit var sampleStore: Store

    @BeforeEach
    fun setUp() {
        sampleStore = Store(
            id = 1L,
            name = "맛있는 카페",
            category = "카페",
            address = "서울시 강남구 역삼동 123-4",
            phone = "02-1234-5678",
            description = "아늑한 분위기의 카페입니다",
            openTime = LocalTime.of(9, 0),
            closeTime = LocalTime.of(22, 0),
            closedDays = mutableListOf("일요일"),
            kakaoChannelId = "kakao_cafe_01",
            ownerEmail = "owner@example.com"
        )
    }

    @Nested
    @DisplayName("매장 조회")
    inner class GetStore {

        @Test
        @DisplayName("ID로 매장을 조회하면 StoreResponse를 반환한다")
        fun `should return store response when store exists`() {
            // given
            whenever(storeRepository.findById(1L)).thenReturn(Optional.of(sampleStore))

            // when
            val result = storeService.getStore(1L)

            // then
            assertEquals(1L, result.id)
            assertEquals("맛있는 카페", result.name)
            assertEquals("카페", result.category)
            assertEquals("서울시 강남구 역삼동 123-4", result.address)
            assertEquals("02-1234-5678", result.phone)
            assertEquals("kakao_cafe_01", result.kakaoChannelId)
            verify(storeRepository).findById(1L)
        }

        @Test
        @DisplayName("존재하지 않는 매장 ID로 조회하면 예외가 발생한다")
        fun `should throw exception when store not found`() {
            // given
            whenever(storeRepository.findById(999L)).thenReturn(Optional.empty())

            // when & then
            val exception = assertThrows(NoSuchElementException::class.java) {
                storeService.getStore(999L)
            }
            assertTrue(exception.message!!.contains("매장을 찾을 수 없습니다"))
        }
    }

    @Nested
    @DisplayName("사장님 매장 목록 조회")
    inner class GetStoresByOwner {

        @Test
        @DisplayName("사장님 이메일로 매장 목록을 조회한다")
        fun `should return stores by owner email`() {
            // given
            val store2 = Store(
                id = 2L, name = "맛있는 식당", category = "한식",
                address = "서울시 강남구 삼성동 456-7", phone = "02-9876-5432",
                ownerEmail = "owner@example.com"
            )
            whenever(storeRepository.findByOwnerEmail("owner@example.com"))
                .thenReturn(listOf(sampleStore, store2))

            // when
            val result = storeService.getStoresByOwner("owner@example.com")

            // then
            assertEquals(2, result.size)
            assertEquals("맛있는 카페", result[0].name)
            assertEquals("맛있는 식당", result[1].name)
        }

        @Test
        @DisplayName("매장이 없는 사장님은 빈 리스트를 반환한다")
        fun `should return empty list when owner has no stores`() {
            // given
            whenever(storeRepository.findByOwnerEmail("nostore@example.com"))
                .thenReturn(emptyList())

            // when
            val result = storeService.getStoresByOwner("nostore@example.com")

            // then
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("매장 등록")
    inner class CreateStore {

        @Test
        @DisplayName("유효한 요청으로 매장을 등록하면 StoreResponse를 반환한다")
        fun `should create store and return response`() {
            // given
            val request = StoreCreateRequest(
                name = "새로운 카페",
                category = "카페",
                address = "서울시 서초구 서초동 789-0",
                phone = "02-5555-6666",
                description = "새로 오픈한 카페",
                openTime = LocalTime.of(8, 0),
                closeTime = LocalTime.of(23, 0),
                closedDays = listOf("월요일"),
                kakaoChannelId = "kakao_new_cafe",
                ownerEmail = "newowner@example.com"
            )
            whenever(storeRepository.save(any<Store>())).thenAnswer { invocation ->
                val saved = invocation.getArgument<Store>(0)
                Store(
                    id = 10L, name = saved.name, category = saved.category,
                    address = saved.address, phone = saved.phone,
                    description = saved.description, openTime = saved.openTime,
                    closeTime = saved.closeTime, closedDays = saved.closedDays,
                    kakaoChannelId = saved.kakaoChannelId, ownerEmail = saved.ownerEmail
                )
            }

            // when
            val result = storeService.createStore(request)

            // then
            assertEquals(10L, result.id)
            assertEquals("새로운 카페", result.name)
            assertEquals("카페", result.category)
            assertEquals("kakao_new_cafe", result.kakaoChannelId)
            verify(storeRepository).save(any<Store>())
        }
    }

    @Nested
    @DisplayName("매장 수정")
    inner class UpdateStore {

        @Test
        @DisplayName("이름과 카테고리만 수정하면 해당 필드만 변경된다")
        fun `should update only provided fields`() {
            // given
            val request = StoreUpdateRequest(
                name = "변경된 카페 이름",
                category = "디저트카페"
            )
            whenever(storeRepository.findById(1L)).thenReturn(Optional.of(sampleStore))
            whenever(storeRepository.save(any<Store>())).thenAnswer { it.getArgument<Store>(0) }

            // when
            val result = storeService.updateStore(1L, request)

            // then
            assertEquals("변경된 카페 이름", result.name)
            assertEquals("디저트카페", result.category)
            // 나머지 필드는 변경되지 않음
            assertEquals("서울시 강남구 역삼동 123-4", result.address)
            assertEquals("02-1234-5678", result.phone)
        }

        @Test
        @DisplayName("존재하지 않는 매장을 수정하려 하면 예외가 발생한다")
        fun `should throw exception when updating non-existent store`() {
            // given
            whenever(storeRepository.findById(999L)).thenReturn(Optional.empty())

            // when & then
            assertThrows(NoSuchElementException::class.java) {
                storeService.updateStore(999L, StoreUpdateRequest(name = "test"))
            }
        }

        @Test
        @DisplayName("모든 필드를 수정할 수 있다")
        fun `should update all fields when all provided`() {
            // given
            val request = StoreUpdateRequest(
                name = "완전 새 이름",
                category = "일식",
                address = "서울시 종로구",
                phone = "02-0000-0000",
                description = "새 설명",
                openTime = LocalTime.of(11, 0),
                closeTime = LocalTime.of(21, 0),
                closedDays = listOf("화요일", "수요일"),
                kakaoChannelId = "new_channel"
            )
            whenever(storeRepository.findById(1L)).thenReturn(Optional.of(sampleStore))
            whenever(storeRepository.save(any<Store>())).thenAnswer { it.getArgument<Store>(0) }

            // when
            val result = storeService.updateStore(1L, request)

            // then
            assertEquals("완전 새 이름", result.name)
            assertEquals("일식", result.category)
            assertEquals("서울시 종로구", result.address)
            assertEquals("02-0000-0000", result.phone)
            assertEquals("새 설명", result.description)
            assertEquals(LocalTime.of(11, 0), result.openTime)
            assertEquals(LocalTime.of(21, 0), result.closeTime)
            assertEquals(listOf("화요일", "수요일"), result.closedDays)
            assertEquals("new_channel", result.kakaoChannelId)
        }
    }

    @Nested
    @DisplayName("매장 삭제")
    inner class DeleteStore {

        @Test
        @DisplayName("매장 ID로 삭제를 호출하면 repository.deleteById가 실행된다")
        fun `should call deleteById on repository`() {
            // given
            doNothing().whenever(storeRepository).deleteById(1L)

            // when
            storeService.deleteStore(1L)

            // then
            verify(storeRepository).deleteById(1L)
        }
    }
}
