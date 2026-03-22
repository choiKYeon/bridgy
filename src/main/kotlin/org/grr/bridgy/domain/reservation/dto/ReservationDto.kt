package org.grr.bridgy.domain.reservation.dto

import org.grr.bridgy.domain.reservation.entity.Reservation
import org.grr.bridgy.domain.reservation.entity.ReservationSource
import org.grr.bridgy.domain.reservation.entity.ReservationStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 사장님 대시보드에서 수동 예약 등록 시 사용
 */
data class ReservationCreateRequest(
    val storeId: Long,
    val customerId: Long,
    val reservationDate: LocalDate,
    val reservationTime: LocalTime,
    val partySize: Int = 1,
    val memo: String? = null,
    val source: ReservationSource = ReservationSource.DASHBOARD
)

data class ReservationResponse(
    val id: Long,
    val storeId: Long,
    val customerId: Long,
    val reservationDate: LocalDate,
    val reservationTime: LocalTime,
    val partySize: Int,
    val memo: String?,
    val status: ReservationStatus,
    val source: ReservationSource,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(reservation: Reservation) = ReservationResponse(
            id = reservation.id,
            storeId = reservation.storeId,
            customerId = reservation.customerId,
            reservationDate = reservation.reservationDate,
            reservationTime = reservation.reservationTime,
            partySize = reservation.partySize,
            memo = reservation.memo,
            status = reservation.status,
            source = reservation.source,
            createdAt = reservation.createdAt
        )
    }
}
