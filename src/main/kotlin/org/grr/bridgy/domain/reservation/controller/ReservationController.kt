package org.grr.bridgy.domain.reservation.controller

import org.grr.bridgy.domain.reservation.dto.ReservationCreateRequest
import org.grr.bridgy.domain.reservation.dto.ReservationResponse
import org.grr.bridgy.domain.reservation.entity.ReservationStatus
import org.grr.bridgy.domain.reservation.service.ReservationService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/reservations")
class ReservationController(
    private val reservationService: ReservationService
) {
    @GetMapping("/{id}")
    fun getReservation(@PathVariable id: Long): ResponseEntity<ReservationResponse> {
        return ResponseEntity.ok(reservationService.getReservation(id))
    }

    @GetMapping("/store/{storeId}")
    fun getReservationsByStore(@PathVariable storeId: Long): ResponseEntity<List<ReservationResponse>> {
        return ResponseEntity.ok(reservationService.getReservationsByStore(storeId))
    }

    @GetMapping("/customer/{customerId}")
    fun getReservationsByCustomer(@PathVariable customerId: Long): ResponseEntity<List<ReservationResponse>> {
        return ResponseEntity.ok(reservationService.getReservationsByCustomer(customerId))
    }

    @PostMapping
    fun createReservation(@RequestBody request: ReservationCreateRequest): ResponseEntity<ReservationResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservationService.createReservation(request))
    }

    @PatchMapping("/{id}/status")
    fun updateStatus(
        @PathVariable id: Long,
        @RequestParam status: ReservationStatus
    ): ResponseEntity<ReservationResponse> {
        return ResponseEntity.ok(reservationService.updateStatus(id, status))
    }

    @PostMapping("/{id}/cancel")
    fun cancelReservation(@PathVariable id: Long): ResponseEntity<ReservationResponse> {
        return ResponseEntity.ok(reservationService.cancelReservation(id))
    }
}
