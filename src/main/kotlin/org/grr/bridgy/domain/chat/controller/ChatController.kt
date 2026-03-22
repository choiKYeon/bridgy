package org.grr.bridgy.domain.chat.controller

import org.grr.bridgy.domain.chat.dto.ChatMessageResponse
import org.grr.bridgy.domain.chat.dto.ChatThreadSummary
import org.grr.bridgy.domain.chat.service.ChatService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 사장님 대시보드 - 대화 내역 API
 */
@RestController
@RequestMapping("/api/dashboard/stores/{storeId}/chats")
class ChatController(
    private val chatService: ChatService
) {
    /**
     * 매장의 전체 대화 내역 (최신순, 페이징)
     */
    @GetMapping
    fun getMessages(
        @PathVariable storeId: Long,
        @PageableDefault(size = 50) pageable: Pageable
    ): ResponseEntity<Page<ChatMessageResponse>> {
        return ResponseEntity.ok(chatService.getMessagesByStore(storeId, pageable))
    }

    /**
     * 고객별 대화 스레드 요약 목록
     */
    @GetMapping("/threads")
    fun getThreads(@PathVariable storeId: Long): ResponseEntity<List<ChatThreadSummary>> {
        return ResponseEntity.ok(chatService.getThreadSummaries(storeId))
    }

    /**
     * 특정 고객과의 대화 상세
     */
    @GetMapping("/user/{kakaoUserId}")
    fun getConversation(
        @PathVariable storeId: Long,
        @PathVariable kakaoUserId: String
    ): ResponseEntity<List<ChatMessageResponse>> {
        return ResponseEntity.ok(chatService.getConversation(storeId, kakaoUserId))
    }

    /**
     * 예약 관련 대화만 필터링
     */
    @GetMapping("/reservations")
    fun getReservationChats(@PathVariable storeId: Long): ResponseEntity<List<ChatMessageResponse>> {
        return ResponseEntity.ok(chatService.getReservationMessages(storeId))
    }
}
