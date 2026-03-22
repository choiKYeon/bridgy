package org.grr.bridgy.domain.reservation.service

import org.grr.bridgy.domain.reservation.dto.ReservationCreateRequest
import org.grr.bridgy.domain.reservation.dto.ReservationResponse
import org.grr.bridgy.domain.reservation.entity.Reservation
import org.grr.bridgy.domain.reservation.entity.ReservationStatus
import org.grr.bridgy.domain.reservation.repository.ReservationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class ReservationService(
    private val reservationRepository: ReservationRepository
) {
    fun getReservation(id: Long): ReservationResponse {
        val reservation = reservationRepository.findById(id)
            .orElseThrow { NoSuchElementException("예약을 찾을 수 없습니다: $id") }
        return ReservationResponse.from(reservation)
    }

    fun getReservationsByStore(storeId: Long): List<ReservationResponse> {
        return reservationRepository.findByStoreId(storeId).map { ReservationResponse.from(it) }
    }

    fun getReservationsByCustomer(customerId: Long): List<ReservationResponse> {
        return reservationRepository.findByCustomerId(customerId).map { ReservationResponse.from(it) }
    }

    @Transactional
    fun createReservation(request: ReservationCreateRequest): ReservationResponse {
        val reservation = Reservation(
            storeId = request.storeId,
            customerId = request.customerId,
            reservationDate = request.reservationDate,
            reservationTime = request.reservationTime,
            partySize = request.partySize,
            memo = request.memo
        )
        return ReservationResponse.from(reservationRepository.save(reservation))
    }

    @Transactional
    fun updateStatus(id: Long, status: ReservationStatus): ReservationResponse {
        val reservation = reservationRepository.findById(id)
            .orElseThrow { NoSuchElementException("예약을 찾을 수 없습니다: $id") }
        reservation.status = status
        reservation.updatedAt = LocalDateTime.now()
        return ReservationResponse.from(reservationRepository.save(reservation))
    }

    @Transactional
    fun cancelReservation(id: Long): ReservationResponse {
        return updateStatus(id, ReservationStatus.CANCELLED)
    }
}
