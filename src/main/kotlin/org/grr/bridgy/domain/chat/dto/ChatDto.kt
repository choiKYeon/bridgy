package org.grr.bridgy.domain.chat.dto

import org.grr.bridgy.domain.chat.entity.ChatMessage
import org.grr.bridgy.domain.chat.entity.MessageSender
import org.grr.bridgy.domain.chat.entity.MessageType
import java.time.LocalDateTime

/**
 * 사장님 대시보드 - 대화 내역 응답
 */
data class ChatMessageResponse(
    val id: Long,
    val kakaoUserId: String,
    val sender: MessageSender,
    val content: String,
    val messageType: MessageType,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(msg: ChatMessage) = ChatMessageResponse(
            id = msg.id,
            kakaoUserId = msg.kakaoUserId,
            sender = msg.sender,
            content = msg.content,
            messageType = msg.messageType,
            createdAt = msg.createdAt
        )
    }
}

/**
 * 사장님 대시보드 - 고객별 대화 요약
 */
data class ChatThreadSummary(
    val kakaoUserId: String,
    val lastMessage: String,
    val lastMessageAt: LocalDateTime,
    val messageCount: Int
)
