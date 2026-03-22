package org.grr.bridgy.config

import org.apache.kafka.clients.admin.NewTopic
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.TopicBuilder

@Configuration
class KafkaConfig {

    companion object {
        const val TOPIC_KAKAO_MESSAGE = "bridgy.kakao.message"
        const val TOPIC_RESERVATION_EVENT = "bridgy.reservation.event"
        const val TOPIC_REVIEW_EVENT = "bridgy.review.event"
        const val TOPIC_NOTIFICATION = "bridgy.notification"
    }

    @Bean
    fun kakaoMessageTopic(): NewTopic = TopicBuilder
        .name(TOPIC_KAKAO_MESSAGE)
        .partitions(3)
        .replicas(1)
        .build()

    @Bean
    fun reservationEventTopic(): NewTopic = TopicBuilder
        .name(TOPIC_RESERVATION_EVENT)
        .partitions(3)
        .replicas(1)
        .build()

    @Bean
    fun reviewEventTopic(): NewTopic = TopicBuilder
        .name(TOPIC_REVIEW_EVENT)
        .partitions(3)
        .replicas(1)
        .build()

    @Bean
    fun notificationTopic(): NewTopic = TopicBuilder
        .name(TOPIC_NOTIFICATION)
        .partitions(3)
        .replicas(1)
        .build()
}
