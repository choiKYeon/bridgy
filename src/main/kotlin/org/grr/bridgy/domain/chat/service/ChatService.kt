package org.grr.bridgy.domain.chat.service

import org.grr.bridgy.domain.chat.dto.ChatMessageResponse
import org.grr.bridgy.domain.chat.dto.ChatThreadSummary
import org.grr.bridgy.domain.chat.entity.MessageType
import org.grr.bridgy.domain.chat.repository.ChatMessageRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 사장님 대시보드용 대화 내역 서비스
 */
@Service
@Transactional(readOnly = true)
class ChatService(
    private val chatMessageRepository: ChatMessageRepository
) {
    /**
     * 매장의 전체 대화 내역 (최신순, 페이징)
     */
    fun getMessagesByStore(storeId: Long, pageable: Pageable): Page<ChatMessageResponse> {
        return chatMessageRepository.findByStoreIdOrderByCreatedAtDesc(storeId, pageable)
            .map { ChatMessageResponse.from(it) }
    }

    /**
     * 특정 고객과의 대화 상세 (시간순)
     */
    fun getConversation(storeId: Long, kakaoUserId: String): List<ChatMessageResponse> {
        return chatMessageRepository
            .findByStoreIdAndKakaoUserIdOrderByCreatedAtAsc(storeId, kakaoUserId)
            .map { ChatMessageResponse.from(it) }
    }

    /**
     * 고객별 대화 요약 목록 (대시보드 메인)
     */
    fun getThreadSummaries(storeId: Long): List<ChatThreadSummary> {
        val allMessages = chatMessageRepository
            .findByStoreIdOrderByCreatedAtDesc(storeId, Pageable.unpaged())
            .content

        return allMessages
            .groupBy { it.kakaoUserId }
            .map { (kakaoUserId, messages) ->
                val latest = messages.first()
                ChatThreadSummary(
                    kakaoUserId = kakaoUserId,
                    lastMessage = latest.content,
                    lastMessageAt = latest.createdAt,
                    messageCount = messages.size
                )
            }
            .sortedByDescending { it.lastMessageAt }
    }

    /**
     * 예약 관련 대화만 필터링
     */
    fun getReservationMessages(storeId: Long): List<ChatMessageResponse> {
        return chatMessageRepository
            .findByStoreIdAndMessageTypeOrderByCreatedAtDesc(storeId, MessageType.RESERVATION)
            .map { ChatMessageResponse.from(it) }
    }
}
