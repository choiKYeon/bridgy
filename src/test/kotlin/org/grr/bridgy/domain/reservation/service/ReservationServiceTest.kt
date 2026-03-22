package org.grr.bridgy.domain.reservation.service

import org.grr.bridgy.domain.reservation.dto.ReservationCreateRequest
import org.grr.bridgy.domain.reservation.entity.Reservation
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
            id = 1L,
            storeId = 10L,
            customerId = 20L,
            reservationDate = LocalDate.of(2026, 4, 1),
            reservationTime = LocalTime.of(18, 30),
            partySize = 4,
            memo = "창가 자리 부탁드립니다",
            status = ReservationStatus.PENDING
        )
    }

    @Nested
    @DisplayName("예약 조회")
    inner class GetReservation {

        @Test
        @DisplayName("예약 ID로 조회하면 ReservationResponse를 반환한다")
        fun `should return reservation when it exists`() {
            // given
            whenever(reservationRepository.findById(1L)).thenReturn(Optional.of(sampleReservation))

            // when
            val result = reservationService.getReservation(1L)

            // then
            assertEquals(1L, result.id)
            assertEquals(10L, result.storeId)
            assertEquals(20L, result.customerId)
            assertEquals(LocalDate.of(2026, 4, 1), result.reservationDate)
            assertEquals(LocalTime.of(18, 30), result.reservationTime)
            assertEquals(4, result.partySize)
            assertEquals("창가 자리 부탁드립니다", result.memo)
            assertEquals(ReservationStatus.PENDING, result.status)
        }

        @Test
        @DisplayName("존재하지 않는 예약 ID로 조회하면 예외가 발생한다")
        fun `should throw exception when reservation not found`() {
            // given
            whenever(reservationRepository.findById(999L)).thenReturn(Optional.empty())

            // when & then
            val exception = assertThrows(NoSuchElementException::class.java) {
                reservationService.getReservation(999L)
            }
            assertTrue(exception.message!!.contains("예약을 찾을 수 없습니다"))
        }
    }

    @Nested
    @DisplayName("매장별 예약 목록 조회")
    inner class GetReservationsByStore {

        @Test
        @DisplayName("매장 ID로 해당 매장의 예약 목록을 조회한다")
        fun `should return reservations for a store`() {
            // given
            val reservation2 = Reservation(
                id = 2L, storeId = 10L, customerId = 30L,
                reservationDate = LocalDate.of(2026, 4, 1),
                reservationTime = LocalTime.of(19, 0), partySize = 2
            )
            whenever(reservationRepository.findByStoreId(10L))
                .thenReturn(listOf(sampleReservation, reservation2))

            // when
            val result = reservationService.getReservationsByStore(10L)

            // then
            assertEquals(2, result.size)
            assertTrue(result.all { it.storeId == 10L })
        }

        @Test
        @DisplayName("예약이 없는 매장은 빈 리스트를 반환한다")
        fun `should return empty list when no reservations`() {
            // given
            whenever(reservationRepository.findByStoreId(99L)).thenReturn(emptyList())

            // when
            val result = reservationService.getReservationsByStore(99L)

            // then
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("고객별 예약 목록 조회")
    inner class GetReservationsByCustomer {

        @Test
        @DisplayName("고객 ID로 예약 목록을 조회한다")
        fun `should return reservations for a customer`() {
            // given
            whenever(reservationRepository.findByCustomerId(20L))
                .thenReturn(listOf(sampleReservation))

            // when
            val result = reservationService.getReservationsByCustomer(20L)

            // then
            assertEquals(1, result.size)
            assertEquals(20L, result[0].customerId)
        }
    }

    @Nested
    @DisplayName("예약 생성")
    inner class CreateReservation {

        @Test
        @DisplayName("유효한 요청으로 예약을 생성하면 PENDING 상태로 저장된다")
        fun `should create reservation with PENDING status`() {
            // given
            val request = ReservationCreateRequest(
                storeId = 10L,
                customerId = 20L,
                reservationDate = LocalDate.of(2026, 4, 5),
                reservationTime = LocalTime.of(12, 0),
                partySize = 3,
                memo = "알레르기 있어요"
            )
            whenever(reservationRepository.save(any<Reservation>())).thenAnswer { invocation ->
                val saved = invocation.getArgument<Reservation>(0)
                Reservation(
                    id = 5L, storeId = saved.storeId, customerId = saved.customerId,
                    reservationDate = saved.reservationDate, reservationTime = saved.reservationTime,
                    partySize = saved.partySize, memo = saved.memo, status = saved.status
                )
            }

            // when
            val result = reservationService.createReservation(request)

            // then
            assertEquals(5L, result.id)
            assertEquals(10L, result.storeId)
            assertEquals(20L, result.customerId)
            assertEquals(3, result.partySize)
            assertEquals("알레르기 있어요", result.memo)
            assertEquals(ReservationStatus.PENDING, result.status)
            verify(reservationRepository).save(any<Reservation>())
        }

        @Test
        @DisplayName("메모 없이 예약을 생성할 수 있다")
        fun `should create reservation without memo`() {
            // given
            val request = ReservationCreateRequest(
                storeId = 10L,
                customerId = 20L,
                reservationDate = LocalDate.of(2026, 4, 5),
                reservationTime = LocalTime.of(12, 0),
                partySize = 1
            )
            whenever(reservationRepository.save(any<Reservation>())).thenAnswer { invocation ->
                val saved = invocation.getArgument<Reservation>(0)
                Reservation(
                    id = 6L, storeId = saved.storeId, customerId = saved.customerId,
                    reservationDate = saved.reservationDate, reservationTime = saved.reservationTime,
                    partySize = saved.partySize, memo = saved.memo
                )
            }

            // when
            val result = reservationService.createReservation(request)

            // then
            assertNull(result.memo)
            assertEquals(1, result.partySize)
        }
    }

    @Nested
    @DisplayName("예약 상태 변경")
    inner class UpdateStatus {

        @Test
        @DisplayName("PENDING 상태를 CONFIRMED로 변경할 수 있다")
        fun `should update status from PENDING to CONFIRMED`() {
            // given
            whenever(reservationRepository.findById(1L)).thenReturn(Optional.of(sampleReservation))
            whenever(reservationRepository.save(any<Reservation>())).thenAnswer { it.getArgument<Reservation>(0) }

            // when
            val result = reservationService.updateStatus(1L, ReservationStatus.CONFIRMED)

            // then
            assertEquals(ReservationStatus.CONFIRMED, result.status)
            verify(reservationRepository).save(any<Reservation>())
        }

        @Test
        @DisplayName("PENDING 상태를 COMPLETED로 변경할 수 있다")
        fun `should update status to COMPLETED`() {
            // given
            whenever(reservationRepository.findById(1L)).thenReturn(Optional.of(sampleReservation))
            whenever(reservationRepository.save(any<Reservation>())).thenAnswer { it.getArgument<Reservation>(0) }

            // when
            val result = reservationService.updateStatus(1L, ReservationStatus.COMPLETED)

            // then
            assertEquals(ReservationStatus.COMPLETED, result.status)
        }

        @Test
        @DisplayName("NO_SHOW 상태로 변경할 수 있다")
        fun `should update status to NO_SHOW`() {
            // given
            whenever(reservationRepository.findById(1L)).thenReturn(Optional.of(sampleReservation))
            whenever(reservationRepository.save(any<Reservation>())).thenAnswer { it.getArgument<Reservation>(0) }

            // when
            val result = reservationService.updateStatus(1L, ReservationStatus.NO_SHOW)

            // then
            assertEquals(ReservationStatus.NO_SHOW, result.status)
        }

        @Test
        @DisplayName("존재하지 않는 예약의 상태를 변경하면 예외가 발생한다")
        fun `should throw exception when reservation not found`() {
            // given
            whenever(reservationRepository.findById(999L)).thenReturn(Optional.empty())

            // when & then
            assertThrows(NoSuchElementException::class.java) {
                reservationService.updateStatus(999L, ReservationStatus.CONFIRMED)
            }
        }
    }

    @Nested
    @DisplayName("예약 취소")
    inner class CancelReservation {

        @Test
        @DisplayName("예약을 취소하면 CANCELLED 상태로 변경된다")
        fun `should set status to CANCELLED`() {
            // given
            whenever(reservationRepository.findById(1L)).thenReturn(Optional.of(sampleReservation))
            whenever(reservationRepository.save(any<Reservation>())).thenAnswer { it.getArgument<Reservation>(0) }

            // when
            val result = reservationService.cancelReservation(1L)

            // then
            assertEquals(ReservationStatus.CANCELLED, result.status)
        }

        @Test
        @DisplayName("존재하지 않는 예약을 취소하면 예외가 발생한다")
        fun `should throw exception when cancelling non-existent reservation`() {
            // given
            whenever(reservationRepository.findById(999L)).thenReturn(Optional.empty())

            // when & then
            assertThrows(NoSuchElementException::class.java) {
                reservationService.cancelReservation(999L)
            }
        }
    }
}
