package org.grr.bridgy.common.config

import org.apache.kafka.clients.admin.NewTopic
import org.grr.bridgy.common.kafka.KafkaTopics
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.kafka.config.TopicBuilder

@Configuration
@Profile("!test")
class KafkaConfig {

    @Bean
    fun notificationTopic(): NewTopic =
        TopicBuilder.name(KafkaTopics.NOTIFICATION)
            .partitions(3)
            .replicas(1)
            .build()

    @Bean
    fun paymentTopic(): NewTopic =
        TopicBuilder.name(KafkaTopics.PAYMENT)
            .partitions(3)
            .replicas(1)
            .build()

    @Bean
    fun cacheInvalidateTopic(): NewTopic =
        TopicBuilder.name(KafkaTopics.CACHE_INVALIDATE)
            .partitions(1)
            .replicas(1)
            .build()
}