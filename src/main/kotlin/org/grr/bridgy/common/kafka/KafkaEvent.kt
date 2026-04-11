package org.grr.bridgy.common.kafka

data class KafkaEvent(
    val eventType: String,
    val payload: Map<String, Any?>
)

// eventType 상수
object EventType {
    // 알림
    const val LIKE_TOGGLED      = "LIKE_TOGGLED"
    const val COMMENT_CREATED   = "COMMENT_CREATED"
    const val COMMENT_DELETED   = "COMMENT_DELETED"

    // 캐시 무효화
    const val PET_UPDATED       = "PET_UPDATED"
    const val PET_DELETED       = "PET_DELETED"

    // 결제 (미래)
    const val PAYMENT_REQUESTED = "PAYMENT_REQUESTED"
    const val PAYMENT_COMPLETED = "PAYMENT_COMPLETED"
    const val PAYMENT_FAILED    = "PAYMENT_FAILED"
}