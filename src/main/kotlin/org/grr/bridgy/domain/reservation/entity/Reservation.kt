package org.grr.bridgy.domain.reservation.entity

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity
@Table(name = "reservations")
class Reservation(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "store_id", nullable = false)
    val storeId: Long,

    @Column(name = "customer_id", nullable = false)
    val customerId: Long,

    @Column(name = "reservation_date", nullable = false)
    var reservationDate: LocalDate,

    @Column(name = "reservation_time", nullable = false)
    var reservationTime: LocalTime,

    @Column(name = "party_size", nullable = false)
    var partySize: Int = 1,

    @Column(length = 500)
    var memo: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ReservationStatus = ReservationStatus.PENDING,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class ReservationStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
    COMPLETED,
    NO_SHOW
}
