package org.grr.bridgy.domain.chat.controller

import org.grr.bridgy.domain.chat.dto.ChatMessageResponse
import org.grr.bridgy.domain.chat.dto.ChatThreadSummary
import org.grr.bridgy.domain.chat.entity.MessageSender
import org.grr.bridgy.domain.chat.entity.MessageType
import org.grr.bridgy.domain.chat.service.ChatService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.bean.MockBean
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDateTime

@WebMvcTest(ChatController::class)
@DisplayName("ChatController API 테스트 (사장님 대시보드)")
class ChatControllerTest {

    @Autowired lateinit var mockMvc: MockMvc
    @MockBean lateinit var chatService: ChatService

    private val now = LocalDateTime.of(2026, 3, 22, 10, 0)

    @Nested
    @DisplayName("GET /api/dashboard/stores/{storeId}/chats")
    inner class GetMessages {

        @Test
        @WithMockUser
        @DisplayName("매장의 대화 내역을 페이징으로 조회한다")
        fun `should return paginated chat messages`() {
            val messages = listOf(
                ChatMessageResponse(1L, "user1", MessageSender.CUSTOMER, "안녕하세요", MessageType.GENERAL, now),
                ChatMessageResponse(2L, "user1", MessageSender.AI, "안녕하세요!", MessageType.GENERAL, now)
            )
            whenever(chatService.getMessagesByStore(eq(10L), any<Pageable>()))
                .thenReturn(PageImpl(messages))

            mockMvc.perform(get("/api/dashboard/stores/10/chats"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].sender").value("CUSTOMER"))
                .andExpect(jsonPath("$.content[1].sender").value("AI"))
        }
    }

    @Nested
    @DisplayName("GET /api/dashboard/stores/{storeId}/chats/threads")
    inner class GetThreads {

        @Test
        @WithMockUser
        @DisplayName("고객별 대화 스레드 요약을 조회한다")
        fun `should return thread summaries`() {
            val summaries = listOf(
                ChatThreadSummary("user1", "예약할게요", now, 5),
                ChatThreadSummary("user2", "안녕하세요", now.minusHours(1), 2)
            )
            whenever(chatService.getThreadSummaries(10L)).thenReturn(summaries)

            mockMvc.perform(get("/api/dashboard/stores/10/chats/threads"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].kakaoUserId").value("user1"))
                .andExpect(jsonPath("$[0].messageCount").value(5))
        }
    }

    @Nested
    @DisplayName("GET /api/dashboard/stores/{storeId}/chats/user/{kakaoUserId}")
    inner class GetConversation {

        @Test
        @WithMockUser
        @DisplayName("특정 고객과의 대화 상세를 조회한다")
        fun `should return conversation with specific user`() {
            val messages = listOf(
                ChatMessageResponse(1L, "user1", MessageSender.CUSTOMER, "영업시간?", MessageType.BUSINESS_INFO, now),
                ChatMessageResponse(2L, "user1", MessageSender.AI, "9시~22시입니다", MessageType.BUSINESS_INFO, now)
            )
            whenever(chatService.getConversation(10L, "user1")).thenReturn(messages)

            mockMvc.perform(get("/api/dashboard/stores/10/chats/user/user1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].content").value("영업시간?"))
                .andExpect(jsonPath("$[1].content").value("9시~22시입니다"))
        }
    }

    @Nested
    @DisplayName("GET /api/dashboard/stores/{storeId}/chats/reservations")
    inner class GetReservationChats {

        @Test
        @WithMockUser
        @DisplayName("예약 관련 대화만 필터링하여 조회한다")
        fun `should return only reservation chats`() {
            val messages = listOf(
                ChatMessageResponse(1L, "user1", MessageSender.CUSTOMER, "내일 6시 2명 예약", MessageType.RESERVATION, now)
            )
            whenever(chatService.getReservationMessages(10L)).thenReturn(messages)

            mockMvc.perform(get("/api/dashboard/stores/10/chats/reservations"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].messageType").value("RESERVATION"))
        }
    }
}
