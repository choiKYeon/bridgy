package org.grr.bridgy.domain.reservation.dto

import org.grr.bridgy.domain.reservation.entity.Reservation
import org.grr.bridgy.domain.reservation.entity.ReservationStatus
import java.time.LocalDate
import java.time.LocalTime

data class ReservationCreateRequest(
    val storeId: Long,
    val customerId: Long,
    val reservationDate: LocalDate,
    val reservationTime: LocalTime,
    val partySize: Int = 1,
    val memo: String? = null
)

data class ReservationResponse(
    val id: Long,
    val storeId: Long,
    val customerId: Long,
    val reservationDate: LocalDate,
    val reservationTime: LocalTime,
    val partySize: Int,
    val memo: String?,
    val status: ReservationStatus
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
            status = reservation.status
        )
    }
}
