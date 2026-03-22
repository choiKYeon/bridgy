package org.grr.bridgy.domain.chat.repository

import org.grr.bridgy.domain.chat.entity.ChatMessage
import org.grr.bridgy.domain.chat.entity.MessageType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ChatMessageRepository : JpaRepository<ChatMessage, Long> {

    /** 매장의 전체 대화 내역 (최신순, 페이징) */
    fun findByStoreIdOrderByCreatedAtDesc(storeId: Long, pageable: Pageable): Page<ChatMessage>

    /** 특정 고객과의 대화 내역 */
    fun findByStoreIdAndKakaoUserIdOrderByCreatedAtAsc(storeId: Long, kakaoUserId: String): List<ChatMessage>

    /** 매장의 특정 타입 메시지만 조회 */
    fun findByStoreIdAndMessageTypeOrderByCreatedAtDesc(storeId: Long, messageType: MessageType): List<ChatMessage>

    /** 매장별 고유 고객 수 */
    @Query("SELECT COUNT(DISTINCT c.kakaoUserId) FROM ChatMessage c WHERE c.storeId = :storeId")
    fun countDistinctCustomersByStoreId(storeId: Long): Long
}
