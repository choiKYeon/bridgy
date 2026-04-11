package org.grr.bridgy.common.kafka

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
@Profile("!test")
class EventPublisher(private val kafkaTemplate: KafkaTemplate<String, KafkaEvent>) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun publish(topic: String, eventType: String, payload: Map<String, Any?>) {
        val event = KafkaEvent(eventType = eventType, payload = payload)
        kafkaTemplate.send(topic, eventType, event)
            .whenComplete { _, ex ->
                if (ex != null) log.error("Kafka 이벤트 발행 실패 [topic=$topic, type=$eventType]", ex)
            }
    }
}