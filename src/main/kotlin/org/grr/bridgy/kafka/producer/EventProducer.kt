package org.grr.bridgy.kafka.producer

import com.fasterxml.jackson.databind.ObjectMapper
import org.grr.bridgy.config.KafkaConfig
import org.grr.bridgy.kafka.event.*
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

/**
 * Kafka 이벤트 발행자
 * 각 도메인 서비스에서 이벤트 발생 시 호출
 */
@Component
class EventProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun publishKakaoMessage(event: KakaoMessageEvent) {
        publish(KafkaConfig.TOPIC_KAKAO_MESSAGE, event.storeId.toString(), event)
    }

    fun publishReservationEvent(event: ReservationEvent) {
        publish(KafkaConfig.TOPIC_RESERVATION_EVENT, event.storeId.toString(), event)
    }

    fun publishReviewEvent(event: ReviewEvent) {
        publish(KafkaConfig.TOPIC_REVIEW_EVENT, event.storeId.toString(), event)
    }

    fun publishNotification(event: NotificationEvent) {
        publish(KafkaConfig.TOPIC_NOTIFICATION, event.storeId.toString(), event)
    }

    private fun publish(topic: String, key: String, event: BridgyEvent) {
        try {
            val payload = objectMapper.writeValueAsString(event)
            kafkaTemplate.send(topic, key, payload)
            log.info("[Kafka] 이벤트 발행 - topic: $topic, type: ${event.eventType}")
        } catch (e: Exception) {
            log.error("[Kafka] 이벤트 발행 실패 - topic: $topic, error: ${e.message}", e)
        }
    }
}
