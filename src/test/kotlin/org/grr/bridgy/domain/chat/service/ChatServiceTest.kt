package org.grr.bridgy.domain.chat.service

import org.grr.bridgy.domain.chat.entity.ChatMessage
import org.grr.bridgy.domain.chat.entity.MessageSender
import org.grr.bridgy.domain.chat.entity.MessageType
import org.grr.bridgy.domain.chat.repository.ChatMessageRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
@DisplayName("ChatService 단위 테스트 (사장님 대시보드)")
class ChatServiceTest {

    @Mock
    lateinit var chatMessageRepository: ChatMessageRepository

    @InjectMocks
    lateinit var chatService: ChatService

    private fun chatMsg(
        id: Long, storeId: Long, kakaoUserId: String,
        sender: MessageSender, content: String,
        type: MessageType = MessageType.GENERAL,
        createdAt: LocalDateTime = LocalDateTime.now()
    ) = ChatMessage(
        id = id, storeId = storeId, kakaoUserId = kakaoUserId,
        sender = sender, content = content, messageType = type,
        createdAt = createdAt
    )

    @Nested
    @DisplayName("매장 전체 대화 내역 조회")
    inner class GetMessagesByStore {

        @Test
        @DisplayName("페이징으로 대화 내역을 조회한다")
        fun `should return paginated chat messages`() {
            val messages = listOf(
                chatMsg(1L, 10L, "user1", MessageSender.CUSTOMER, "안녕하세요"),
                chatMsg(2L, 10L, "user1", MessageSender.AI, "안녕하세요, 맛있는 카페입니다!"),
                chatMsg(3L, 10L, "user2", MessageSender.CUSTOMER, "예약하고 싶어요")
            )
            val pageable = PageRequest.of(0, 50)
            whenever(chatMessageRepository.findByStoreIdOrderByCreatedAtDesc(10L, pageable))
                .thenReturn(PageImpl(messages, pageable, 3))

            val result = chatService.getMessagesByStore(10L, pageable)

            assertEquals(3, result.totalElements)
            assertEquals("안녕하세요", result.content[0].content)
        }
    }

    @Nested
    @DisplayName("특정 고객과의 대화 상세")
    inner class GetConversation {

        @Test
        @DisplayName("특정 고객과의 대화를 시간순으로 조회한다")
        fun `should return conversation in chronological order`() {
            val messages = listOf(
                chatMsg(1L, 10L, "user1", MessageSender.CUSTOMER, "안녕하세요",
                    createdAt = LocalDateTime.of(2026, 3, 22, 10, 0)),
                chatMsg(2L, 10L, "user1", MessageSender.AI, "안녕하세요!",
                    createdAt = LocalDateTime.of(2026, 3, 22, 10, 0, 1)),
                chatMsg(3L, 10L, "user1", MessageSender.CUSTOMER, "예약할게요",
                    createdAt = LocalDateTime.of(2026, 3, 22, 10, 1))
            )
            whenever(chatMessageRepository.findByStoreIdAndKakaoUserIdOrderByCreatedAtAsc(10L, "user1"))
                .thenReturn(messages)

            val result = chatService.getConversation(10L, "user1")

            assertEquals(3, result.size)
            assertEquals(MessageSender.CUSTOMER, result[0].sender)
            assertEquals(MessageSender.AI, result[1].sender)
            assertEquals(MessageSender.CUSTOMER, result[2].sender)
        }

        @Test
        @DisplayName("대화가 없는 고객은 빈 리스트를 반환한다")
        fun `should return empty list for unknown user`() {
            whenever(chatMessageRepository.findByStoreIdAndKakaoUserIdOrderByCreatedAtAsc(10L, "unknown"))
                .thenReturn(emptyList())

            val result = chatService.getConversation(10L, "unknown")
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("고객별 대화 스레드 요약")
    inner class GetThreadSummaries {

        @Test
        @DisplayName("고객별로 마지막 메시지와 메시지 수를 보여준다")
        fun `should group by customer and show summary`() {
            val now = LocalDateTime.now()
            val messages = listOf(
                chatMsg(3L, 10L, "user2", MessageSender.CUSTOMER, "예약할게요",
                    createdAt = now),
                chatMsg(2L, 10L, "user1", MessageSender.AI, "안녕하세요!",
                    createdAt = now.minusMinutes(5)),
                chatMsg(1L, 10L, "user1", MessageSender.CUSTOMER, "안녕하세요",
                    createdAt = now.minusMinutes(10))
            )
            whenever(chatMessageRepository.findByStoreIdOrderByCreatedAtDesc(eq(10L), any<Pageable>()))
                .thenReturn(PageImpl(messages))

            val result = chatService.getThreadSummaries(10L)

            assertEquals(2, result.size)
            // 최신 메시지 기준 정렬
            assertEquals("user2", result[0].kakaoUserId)
            assertEquals("예약할게요", result[0].lastMessage)
            assertEquals(1, result[0].messageCount)
            assertEquals("user1", result[1].kakaoUserId)
            assertEquals(2, result[1].messageCount)
        }
    }

    @Nested
    @DisplayName("예약 관련 대화 필터링")
    inner class GetReservationMessages {

        @Test
        @DisplayName("예약 관련 메시지만 필터링하여 조회한다")
        fun `should return only reservation messages`() {
            val reservationMessages = listOf(
                chatMsg(1L, 10L, "user1", MessageSender.CUSTOMER, "내일 6시 2명 예약",
                    type = MessageType.RESERVATION),
                chatMsg(2L, 10L, "user1", MessageSender.AI, "예약 접수되었습니다!",
                    type = MessageType.RESERVATION)
            )
            whenever(chatMessageRepository.findByStoreIdAndMessageTypeOrderByCreatedAtDesc(10L, MessageType.RESERVATION))
                .thenReturn(reservationMessages)

            val result = chatService.getReservationMessages(10L)

            assertEquals(2, result.size)
            assertTrue(result.all { it.messageType == MessageType.RESERVATION })
        }
    }
}
