package org.grr.bridgy.kafka.event

import java.time.LocalDateTime

/**
 * Kafka 이벤트 공통 인터페이스
 */
sealed class BridgyEvent(
    val eventType: String,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

/**
 * 카카오톡 메시지 수신 이벤트
 * 웹훅 → Kafka → 비동기 AI 응답 생성
 */
data class KakaoMessageEvent(
    val storeId: Long,
    val kakaoUserId: String,
    val message: String
) : BridgyEvent("KAKAO_MESSAGE")

/**
 * 예약 이벤트 (생성/확인/취소)
 * 사장님에게 실시간 알림 전송
 */
data class ReservationEvent(
    val storeId: Long,
    val reservationId: Long,
    val customerId: Long,
    val action: ReservationAction,
    val summary: String
) : BridgyEvent("RESERVATION")

enum class ReservationAction {
    CREATED,    // 새 예약 접수 (카카오톡)
    CONFIRMED,  // 사장님 승인
    CANCELLED,  // 취소
    COMPLETED,  // 방문 완료
    NO_SHOW     // 노쇼
}

/**
 * 리뷰 이벤트 (새 리뷰 등록)
 * 사장님에게 실시간 알림 + AI 자동 답글
 */
data class ReviewEvent(
    val storeId: Long,
    val reviewId: Long,
    val rating: Int,
    val content: String
) : BridgyEvent("REVIEW")

/**
 * 사장님 알림 이벤트
 * Kafka Consumer에서 처리하여 대시보드/푸시 알림
 */
data class NotificationEvent(
    val storeId: Long,
    val ownerEmail: String,
    val title: String,
    val message: String,
    val notificationType: NotificationType
) : BridgyEvent("NOTIFICATION")

enum class NotificationType {
    NEW_INQUIRY,       // 새 문의
    NEW_RESERVATION,   // 새 예약
    NEW_REVIEW,        // 새 리뷰
    RESERVATION_UPDATE // 예약 상태 변경
}
