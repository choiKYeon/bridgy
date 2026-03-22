package org.grr.bridgy.domain.chat.entity

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 카카오톡 대화 내역
 * 고객 메시지와 AI 자동 응답을 모두 저장하여
 * 사장님 대시보드에서 확인 가능
 */
@Entity
@Table(name = "chat_messages", indexes = [
    Index(name = "idx_chat_store_id", columnList = "store_id"),
    Index(name = "idx_chat_kakao_user_id", columnList = "kakao_user_id"),
    Index(name = "idx_chat_created_at", columnList = "created_at")
])
class ChatMessage(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "store_id", nullable = false)
    val storeId: Long,

    @Column(name = "kakao_user_id", nullable = false, length = 100)
    val kakaoUserId: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val sender: MessageSender,

    @Column(nullable = false, length = 2000)
    val content: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    val messageType: MessageType = MessageType.GENERAL,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class MessageSender {
    CUSTOMER,   // 고객이 보낸 메시지
    AI,         // AI 자동 응답
    OWNER       // 사장님 수동 응답 (추후 확장)
}

enum class MessageType {
    GENERAL,        // 일반 문의
    RESERVATION,    // 예약 관련
    REVIEW,         // 리뷰 관련
    BUSINESS_INFO   // 영업정보 문의
}
