package org.grr.bridgy.common.kafka

object KafkaTopics {
    const val NOTIFICATION = "bridgy.notification"   // 좋아요, 댓글 알림
    const val PAYMENT      = "bridgy.payment"         // 결제 이벤트
    const val CACHE_INVALIDATE = "bridgy.cache.invalidate"  // 캐시 무효화
}