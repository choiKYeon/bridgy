package org.grr.bridgy.domain.reservation.controller

import org.grr.bridgy.domain.reservation.dto.ReservationResponse
import org.grr.bridgy.domain.reservation.entity.ReservationStatus
import org.grr.bridgy.domain.reservation.service.ReservationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 사장님 대시보드 - 예약 관리 API
 * 예약 접수는 카카오톡 대화로 자동 생성되고,
 * 사장님은 여기서 확인/승인/거절만 처리
 */
@RestController
@RequestMapping("/api/dashboard/stores/{storeId}/reservations")
class ReservationController(
    private val reservationService: ReservationService
) {
    @GetMapping("/{id}")
    fun getReservation(
        @PathVariable storeId: Long,
        @PathVariable id: Long
    ): ResponseEntity<ReservationResponse> {
        return ResponseEntity.ok(reservationService.getReservation(id))
    }

    @GetMapping
    fun getReservationsByStore(@PathVariable storeId: Long): ResponseEntity<List<ReservationResponse>> {
        return ResponseEntity.ok(reservationService.getReservationsByStore(storeId))
    }

    @PatchMapping("/{id}/confirm")
    fun confirmReservation(
        @PathVariable storeId: Long,
        @PathVariable id: Long
    ): ResponseEntity<ReservationResponse> {
        return ResponseEntity.ok(reservationService.updateStatus(id, ReservationStatus.CONFIRMED))
    }

    @PatchMapping("/{id}/cancel")
    fun cancelReservation(
        @PathVariable storeId: Long,
        @PathVariable id: Long
    ): ResponseEntity<ReservationResponse> {
        return ResponseEntity.ok(reservationService.cancelReservation(id))
    }

    @PatchMapping("/{id}/complete")
    fun completeReservation(
        @PathVariable storeId: Long,
        @PathVariable id: Long
    ): ResponseEntity<ReservationResponse> {
        return ResponseEntity.ok(reservationService.updateStatus(id, ReservationStatus.COMPLETED))
    }

    @PatchMapping("/{id}/no-show")
    fun markNoShow(
        @PathVariable storeId: Long,
        @PathVariable id: Long
    ): ResponseEntity<ReservationResponse> {
        return ResponseEntity.ok(reservationService.updateStatus(id, ReservationStatus.NO_SHOW))
    }
}
