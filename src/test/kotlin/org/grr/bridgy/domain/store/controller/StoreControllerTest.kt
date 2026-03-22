package org.grr.bridgy.domain.store.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.grr.bridgy.domain.store.dto.StoreCreateRequest
import org.grr.bridgy.domain.store.dto.StoreResponse
import org.grr.bridgy.domain.store.dto.StoreUpdateRequest
import org.grr.bridgy.domain.store.service.StoreService
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
import java.time.LocalTime

@WebMvcTest(StoreController::class)
@DisplayName("StoreController API 테스트")
class StoreControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockBean
    lateinit var storeService: StoreService

    private val objectMapper = ObjectMapper().registerModule(JavaTimeModule())

    private val sampleResponse = StoreResponse(
        id = 1L, name = "맛있는 카페", category = "카페",
        address = "서울시 강남구 역삼동", phone = "02-1234-5678",
        description = "아늑한 카페", openTime = LocalTime.of(9, 0),
        closeTime = LocalTime.of(22, 0), closedDays = listOf("일요일"),
        kakaoChannelId = "kakao_cafe_01"
    )

    @Nested
    @DisplayName("GET /api/stores/{id}")
    inner class GetStore {

        @Test
        @WithMockUser
        @DisplayName("매장 ID로 조회 시 200 OK와 매장 정보를 반환한다")
        fun `should return 200 with store info`() {
            // given
            whenever(storeService.getStore(1L)).thenReturn(sampleResponse)

            // when & then
            mockMvc.perform(get("/api/stores/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("맛있는 카페"))
                .andExpect(jsonPath("$.category").value("카페"))
                .andExpect(jsonPath("$.address").value("서울시 강남구 역삼동"))
                .andExpect(jsonPath("$.phone").value("02-1234-5678"))
                .andExpect(jsonPath("$.kakaoChannelId").value("kakao_cafe_01"))
        }

        @Test
        @WithMockUser
        @DisplayName("존재하지 않는 매장 ID로 조회 시 예외가 발생한다")
        fun `should return error when store not found`() {
            // given
            whenever(storeService.getStore(999L)).thenThrow(NoSuchElementException("매장을 찾을 수 없습니다: 999"))

            // when & then
            mockMvc.perform(get("/api/stores/999"))
                .andExpect(status().is5xxServerError)
        }
    }

    @Nested
    @DisplayName("GET /api/stores?ownerEmail=")
    inner class GetStoresByOwner {

        @Test
        @WithMockUser
        @DisplayName("사장님 이메일로 매장 목록을 조회한다")
        fun `should return stores by owner email`() {
            // given
            whenever(storeService.getStoresByOwner("owner@example.com"))
                .thenReturn(listOf(sampleResponse))

            // when & then
            mockMvc.perform(get("/api/stores").param("ownerEmail", "owner@example.com"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("맛있는 카페"))
        }
    }

    @Nested
    @DisplayName("POST /api/stores")
    inner class CreateStore {

        @Test
        @WithMockUser
        @DisplayName("유효한 요청으로 매장을 등록하면 201 Created를 반환한다")
        fun `should return 201 when store created`() {
            // given
            val request = StoreCreateRequest(
                name = "새 카페", category = "카페",
                address = "서울시 서초구", phone = "02-5555-6666",
                ownerEmail = "new@example.com"
            )
            whenever(storeService.createStore(any())).thenReturn(
                sampleResponse.copy(id = 10L, name = "새 카페")
            )

            // when & then
            mockMvc.perform(
                post("/api/stores")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("새 카페"))
        }
    }

    @Nested
    @DisplayName("PUT /api/stores/{id}")
    inner class UpdateStore {

        @Test
        @WithMockUser
        @DisplayName("매장 정보를 수정하면 200 OK를 반환한다")
        fun `should return 200 when store updated`() {
            // given
            val request = StoreUpdateRequest(name = "변경된 이름")
            whenever(storeService.updateStore(eq(1L), any())).thenReturn(
                sampleResponse.copy(name = "변경된 이름")
            )

            // when & then
            mockMvc.perform(
                put("/api/stores/1")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.name").value("변경된 이름"))
        }
    }

    @Nested
    @DisplayName("DELETE /api/stores/{id}")
    inner class DeleteStore {

        @Test
        @WithMockUser
        @DisplayName("매장을 삭제하면 204 No Content를 반환한다")
        fun `should return 204 when store deleted`() {
            // given
            doNothing().whenever(storeService).deleteStore(1L)

            // when & then
            mockMvc.perform(delete("/api/stores/1").with(csrf()))
                .andExpect(status().isNoContent)
        }
    }
}
