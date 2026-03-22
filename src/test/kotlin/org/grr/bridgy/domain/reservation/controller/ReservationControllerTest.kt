package org.grr.bridgy.domain.reservation.controller

import org.grr.bridgy.domain.reservation.dto.ReservationResponse
import org.grr.bridgy.domain.reservation.entity.ReservationSource
import org.grr.bridgy.domain.reservation.entity.ReservationStatus
import org.grr.bridgy.domain.reservation.service.ReservationService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.bean.MockBean
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@WebMvcTest(ReservationController::class)
@DisplayName("ReservationController API 테스트 (사장님 대시보드)")
class ReservationControllerTest {

    @Autowired lateinit var mockMvc: MockMvc
    @MockBean lateinit var reservationService: ReservationService

    private val sampleResponse = ReservationResponse(
        id = 1L, storeId = 10L, customerId = 20L,
        reservationDate = LocalDate.of(2026, 4, 1),
        reservationTime = LocalTime.of(18, 30),
        partySize = 4, memo = "카카오톡 예약 (자동)",
        status = ReservationStatus.PENDING,
        source = ReservationSource.KAKAOTALK,
        createdAt = LocalDateTime.of(2026, 3, 22, 10, 0)
    )

    @Nested
    @DisplayName("GET /api/dashboard/stores/{storeId}/reservations")
    inner class GetReservations {

        @Test
        @WithMockUser
        @DisplayName("매장의 예약 목록을 조회한다")
        fun `should return store reservations`() {
            whenever(reservationService.getReservationsByStore(10L)).thenReturn(listOf(sampleResponse))

            mockMvc.perform(get("/api/dashboard/stores/10/reservations"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].source").value("KAKAOTALK"))
                .andExpect(jsonPath("$[0].partySize").value(4))
        }
    }

    @Nested
    @DisplayName("PATCH /api/dashboard/stores/{storeId}/reservations/{id}/confirm")
    inner class ConfirmReservation {

        @Test
        @WithMockUser
        @DisplayName("예약을 승인하면 CONFIRMED 상태를 반환한다")
        fun `should return CONFIRMED status`() {
            whenever(reservationService.updateStatus(1L, ReservationStatus.CONFIRMED))
                .thenReturn(sampleResponse.copy(status = ReservationStatus.CONFIRMED))

            mockMvc.perform(patch("/api/dashboard/stores/10/reservations/1/confirm").with(csrf()))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
        }
    }

    @Nested
    @DisplayName("PATCH /api/dashboard/stores/{storeId}/reservations/{id}/cancel")
    inner class CancelReservation {

        @Test
        @WithMockUser
        @DisplayName("예약을 취소하면 CANCELLED 상태를 반환한다")
        fun `should return CANCELLED status`() {
            whenever(reservationService.cancelReservation(1L))
                .thenReturn(sampleResponse.copy(status = ReservationStatus.CANCELLED))

            mockMvc.perform(patch("/api/dashboard/stores/10/reservations/1/cancel").with(csrf()))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.status").value("CANCELLED"))
        }
    }

    @Nested
    @DisplayName("PATCH .../complete, .../no-show")
    inner class OtherStatusChanges {

        @Test
        @WithMockUser
        @DisplayName("방문 완료 처리")
        fun `should complete reservation`() {
            whenever(reservationService.updateStatus(1L, ReservationStatus.COMPLETED))
                .thenReturn(sampleResponse.copy(status = ReservationStatus.COMPLETED))

            mockMvc.perform(patch("/api/dashboard/stores/10/reservations/1/complete").with(csrf()))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.status").value("COMPLETED"))
        }

        @Test
        @WithMockUser
        @DisplayName("노쇼 처리")
        fun `should mark no show`() {
            whenever(reservationService.updateStatus(1L, ReservationStatus.NO_SHOW))
                .thenReturn(sampleResponse.copy(status = ReservationStatus.NO_SHOW))

            mockMvc.perform(patch("/api/dashboard/stores/10/reservations/1/no-show").with(csrf()))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.status").value("NO_SHOW"))
        }
    }
}
