package org.grr.bridgy.domain.reservation.repository

import org.grr.bridgy.domain.reservation.entity.Reservation
import org.grr.bridgy.domain.reservation.entity.ReservationStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface ReservationRepository : JpaRepository<Reservation, Long> {
    fun findByStoreIdAndReservationDate(storeId: Long, date: LocalDate): List<Reservation>
    fun findByStoreIdAndStatus(storeId: Long, status: ReservationStatus): List<Reservation>
    fun findByCustomerId(customerId: Long): List<Reservation>
    fun findByStoreId(storeId: Long): List<Reservation>
}
