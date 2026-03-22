package org.grr.bridgy.kafka.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import org.grr.bridgy.config.KafkaConfig
import org.grr.bridgy.kafka.event.NotificationEvent
import org.grr.bridgy.kafka.event.ReservationEvent
import org.grr.bridgy.kafka.event.ReviewEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

/**
 * Kafka 이벤트 소비자
 * 이벤트를 수신하여 사장님에게 알림 전송
 */
@Component
class NotificationConsumer(
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 새 예약 이벤트 수신 → 사장님 알림
     */
    @KafkaListener(
        topics = [KafkaConfig.TOPIC_RESERVATION_EVENT],
        groupId = "bridgy-notification"
    )
    fun handleReservationEvent(payload: String) {
        try {
            val event = objectMapper.readValue(payload, ReservationEvent::class.java)
            log.info("[알림] 예약 이벤트 수신 - storeId: ${event.storeId}, action: ${event.action}, summary: ${event.summary}")

            // TODO: 실제 알림 구현 (웹소켓, 푸시, 카카오 알림톡 등)
            // 현재는 로그만 출력
        } catch (e: Exception) {
            log.error("[알림] 예약 이벤트 처리 실패: ${e.message}", e)
        }
    }

    /**
     * 새 리뷰 이벤트 수신 → 사장님 알림
     */
    @KafkaListener(
        topics = [KafkaConfig.TOPIC_REVIEW_EVENT],
        groupId = "bridgy-notification"
    )
    fun handleReviewEvent(payload: String) {
        try {
            val event = objectMapper.readValue(payload, ReviewEvent::class.java)
            log.info("[알림] 리뷰 이벤트 수신 - storeId: ${event.storeId}, rating: ${event.rating}")

            // TODO: 실제 알림 구현
        } catch (e: Exception) {
            log.error("[알림] 리뷰 이벤트 처리 실패: ${e.message}", e)
        }
    }

    /**
     * 범용 알림 이벤트 수신
     */
    @KafkaListener(
        topics = [KafkaConfig.TOPIC_NOTIFICATION],
        groupId = "bridgy-notification"
    )
    fun handleNotification(payload: String) {
        try {
            val event = objectMapper.readValue(payload, NotificationEvent::class.java)
            log.info("[알림] ${event.notificationType} - ${event.title}: ${event.message}")

            // TODO: 실제 알림 전송
        } catch (e: Exception) {
            log.error("[알림] 알림 처리 실패: ${e.message}", e)
        }
    }
}
