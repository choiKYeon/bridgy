package org.grr.bridgy.domain.reservation.service

import org.grr.bridgy.domain.reservation.dto.ReservationCreateRequest
import org.grr.bridgy.domain.reservation.entity.Reservation
import org.grr.bridgy.domain.reservation.entity.ReservationSource
import org.grr.bridgy.domain.reservation.entity.ReservationStatus
import org.grr.bridgy.domain.reservation.repository.ReservationRepository
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
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("ReservationService 단위 테스트")
class ReservationServiceTest {

    @Mock
    lateinit var reservationRepository: ReservationRepository

    @InjectMocks
    lateinit var reservationService: ReservationService

    private lateinit var sampleReservation: Reservation

    @BeforeEach
    fun setUp() {
        sampleReservation = Reservation(
            id = 1L, storeId = 10L, customerId = 20L,
            reservationDate = LocalDate.of(2026, 4, 1),
            reservationTime = LocalTime.of(18, 30),
            partySize = 4, memo = "카카오톡 예약 (자동)",
            status = ReservationStatus.PENDING,
            source = ReservationSource.KAKAOTALK
        )
    }

    @Nested
    @DisplayName("예약 조회")
    inner class GetReservation {

        @Test
        @DisplayName("예약 ID로 조회하면 ReservationResponse를 반환한다")
        fun `should return reservation when it exists`() {
            whenever(reservationRepository.findById(1L)).thenReturn(Optional.of(sampleReservation))

            val result = reservationService.getReservation(1L)

            assertEquals(1L, result.id)
            assertEquals(ReservationStatus.PENDING, result.status)
            assertEquals(ReservationSource.KAKAOTALK, result.source)
            assertEquals(4, result.partySize)
        }

        @Test
        @DisplayName("존재하지 않는 예약 ID로 조회하면 예외가 발생한다")
        fun `should throw exception when reservation not found`() {
            whenever(reservationRepository.findById(999L)).thenReturn(Optional.empty())

            assertThrows(NoSuchElementException::class.java) {
                reservationService.getReservation(999L)
            }
        }
    }

    @Nested
    @DisplayName("매장별 예약 목록 조회")
    inner class GetReservationsByStore {

        @Test
        @DisplayName("매장 ID로 예약 목록을 조회한다")
        fun `should return reservations for a store`() {
            whenever(reservationRepository.findByStoreId(10L)).thenReturn(listOf(sampleReservation))

            val result = reservationService.getReservationsByStore(10L)

            assertEquals(1, result.size)
            assertEquals(10L, result[0].storeId)
        }
    }

    @Nested
    @DisplayName("사장님 예약 상태 변경")
    inner class UpdateStatus {

        @Test
        @DisplayName("PENDING → CONFIRMED 승인 처리")
        fun `should confirm reservation`() {
            whenever(reservationRepository.findById(1L)).thenReturn(Optional.of(sampleReservation))
            whenever(reservationRepository.save(any<Reservation>())).thenAnswer { it.getArgument<Reservation>(0) }

            val result = reservationService.updateStatus(1L, ReservationStatus.CONFIRMED)
            assertEquals(ReservationStatus.CONFIRMED, result.status)
        }

        @Test
        @DisplayName("PENDING → CANCELLED 취소 처리")
        fun `should cancel reservation`() {
            whenever(reservationRepository.findById(1L)).thenReturn(Optional.of(sampleReservation))
            whenever(reservationRepository.save(any<Reservation>())).thenAnswer { it.getArgument<Reservation>(0) }

            val result = reservationService.cancelReservation(1L)
            assertEquals(ReservationStatus.CANCELLED, result.status)
        }

        @Test
        @DisplayName("COMPLETED 방문 완료 처리")
        fun `should complete reservation`() {
            whenever(reservationRepository.findById(1L)).thenReturn(Optional.of(sampleReservation))
            whenever(reservationRepository.save(any<Reservation>())).thenAnswer { it.getArgument<Reservation>(0) }

            val result = reservationService.updateStatus(1L, ReservationStatus.COMPLETED)
            assertEquals(ReservationStatus.COMPLETED, result.status)
        }

        @Test
        @DisplayName("NO_SHOW 노쇼 처리")
        fun `should mark no show`() {
            whenever(reservationRepository.findById(1L)).thenReturn(Optional.of(sampleReservation))
            whenever(reservationRepository.save(any<Reservation>())).thenAnswer { it.getArgument<Reservation>(0) }

            val result = reservationService.updateStatus(1L, ReservationStatus.NO_SHOW)
            assertEquals(ReservationStatus.NO_SHOW, result.status)
        }

        @Test
        @DisplayName("존재하지 않는 예약 상태 변경 시 예외 발생")
        fun `should throw for non-existent reservation`() {
            whenever(reservationRepository.findById(999L)).thenReturn(Optional.empty())

            assertThrows(NoSuchElementException::class.java) {
                reservationService.updateStatus(999L, ReservationStatus.CONFIRMED)
            }
        }
    }
}
