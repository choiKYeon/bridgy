package org.grr.bridgy.kakao.service

import org.grr.bridgy.ai.service.AiService
import org.grr.bridgy.ai.service.ReservationParseResult
import org.grr.bridgy.domain.chat.entity.ChatMessage
import org.grr.bridgy.domain.chat.entity.MessageSender
import org.grr.bridgy.domain.chat.entity.MessageType
import org.grr.bridgy.domain.chat.repository.ChatMessageRepository
import org.grr.bridgy.domain.customer.entity.Customer
import org.grr.bridgy.domain.customer.repository.CustomerRepository
import org.grr.bridgy.domain.reservation.entity.Reservation
import org.grr.bridgy.domain.reservation.repository.ReservationRepository
import org.grr.bridgy.domain.store.entity.Store
import org.grr.bridgy.domain.store.repository.StoreRepository
import org.grr.bridgy.kafka.producer.EventProducer
import org.grr.bridgy.kakao.dto.KakaoUser
import org.grr.bridgy.kakao.dto.KakaoUserRequest
import org.grr.bridgy.kakao.dto.KakaoWebhookRequest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("KakaoChannelService 단위 테스트")
class KakaoChannelServiceTest {

    @Mock lateinit var customerRepository: CustomerRepository
    @Mock lateinit var storeRepository: StoreRepository
    @Mock lateinit var chatMessageRepository: ChatMessageRepository
    @Mock lateinit var reservationRepository: ReservationRepository
    @Mock lateinit var aiService: AiService
    @Mock lateinit var eventProducer: EventProducer

    @InjectMocks
    lateinit var kakaoChannelService: KakaoChannelService

    private lateinit var sampleStore: Store
    private lateinit var sampleCustomer: Customer

    @BeforeEach
    fun setUp() {
        sampleStore = Store(
            id = 1L, name = "맛있는 카페", category = "카페",
            address = "서울시 강남구", phone = "02-1234-5678",
            description = "아늑한 분위기의 카페",
            ownerEmail = "owner@example.com"
        )
        sampleCustomer = Customer(id = 10L, kakaoUserId = "kakao_user_001")
    }

    private fun webhookRequest(userId: String, message: String) = KakaoWebhookRequest(
        userRequest = KakaoUserRequest(
            user = KakaoUser(id = userId),
            utterance = message
        )
    )

    @Nested
    @DisplayName("일반 문의 처리")
    inner class GeneralInquiry {

        @Test
        @DisplayName("기존 고객의 일반 문의에 AI 응답을 반환하고 대화 내역을 저장한다")
        fun `should return AI reply and save chat history`() {
            // given
            val request = webhookRequest("kakao_user_001", "안녕하세요")
            whenever(customerRepository.findByKakaoUserId("kakao_user_001")).thenReturn(sampleCustomer)
            whenever(customerRepository.save(any<Customer>())).thenReturn(sampleCustomer)
            whenever(storeRepository.findById(1L)).thenReturn(Optional.of(sampleStore))
            whenever(aiService.classifyIntent("안녕하세요")).thenReturn(MessageType.GENERAL)
            whenever(aiService.generateCustomerReply(any(), any(), any())).thenReturn("안녕하세요, 맛있는 카페입니다!")
            whenever(chatMessageRepository.save(any<ChatMessage>())).thenAnswer { it.getArgument<ChatMessage>(0) }

            // when
            val response = kakaoChannelService.handleMessage(1L, request)

            // then
            val text = response.template.outputs[0].simpleText?.text
            assertEquals("안녕하세요, 맛있는 카페입니다!", text)

            // 대화 내역 저장 검증 (고객 메시지 + AI 응답 = 2회)
            verify(chatMessageRepository, times(2)).save(any<ChatMessage>())
        }
    }

    @Nested
    @DisplayName("신규 고객 자동 생성")
    inner class NewCustomer {

        @Test
        @DisplayName("처음 문의한 고객은 자동으로 생성된다 (회원가입 불필요)")
        fun `should auto-create new customer without registration`() {
            // given
            val request = webhookRequest("new_user_999", "영업시간 알려주세요")
            val newCustomer = Customer(id = 99L, kakaoUserId = "new_user_999")

            whenever(customerRepository.findByKakaoUserId("new_user_999")).thenReturn(null)
            whenever(customerRepository.save(any<Customer>())).thenReturn(newCustomer)
            whenever(storeRepository.findById(1L)).thenReturn(Optional.of(sampleStore))
            whenever(aiService.classifyIntent(any())).thenReturn(MessageType.BUSINESS_INFO)
            whenever(aiService.generateCustomerReply(any(), any(), any())).thenReturn("영업시간 안내")
            whenever(chatMessageRepository.save(any<ChatMessage>())).thenAnswer { it.getArgument<ChatMessage>(0) }

            // when
            kakaoChannelService.handleMessage(1L, request)

            // then - 고객 자동 생성 확인
            verify(customerRepository).save(argThat<Customer> { kakaoUserId == "new_user_999" })
        }

        @Test
        @DisplayName("기존 고객은 lastContactAt이 갱신된다")
        fun `should update lastContactAt for existing customer`() {
            // given
            val request = webhookRequest("kakao_user_001", "안녕")
            whenever(customerRepository.findByKakaoUserId("kakao_user_001")).thenReturn(sampleCustomer)
            whenever(customerRepository.save(any<Customer>())).thenReturn(sampleCustomer)
            whenever(storeRepository.findById(1L)).thenReturn(Optional.of(sampleStore))
            whenever(aiService.classifyIntent(any())).thenReturn(MessageType.GENERAL)
            whenever(aiService.generateCustomerReply(any(), any(), any())).thenReturn("응답")
            whenever(chatMessageRepository.save(any<ChatMessage>())).thenAnswer { it.getArgument<ChatMessage>(0) }

            // when
            kakaoChannelService.handleMessage(1L, request)

            // then - 기존 고객 save 호출 (touch 반영)
            verify(customerRepository).save(sampleCustomer)
        }
    }

    @Nested
    @DisplayName("매장 미존재 처리")
    inner class StoreNotFound {

        @Test
        @DisplayName("존재하지 않는 매장으로 문의하면 에러 메시지를 반환한다")
        fun `should return error when store not found`() {
            // given
            val request = webhookRequest("kakao_user_001", "안녕하세요")
            whenever(customerRepository.findByKakaoUserId("kakao_user_001")).thenReturn(sampleCustomer)
            whenever(customerRepository.save(any<Customer>())).thenReturn(sampleCustomer)
            whenever(storeRepository.findById(999L)).thenReturn(Optional.empty())

            // when
            val response = kakaoChannelService.handleMessage(999L, request)

            // then
            val text = response.template.outputs[0].simpleText?.text
            assertTrue(text!!.contains("매장 정보를 찾을 수 없습니다"))
            verify(chatMessageRepository, never()).save(any())
        }
    }

    @Nested
    @DisplayName("카카오톡 예약 처리")
    inner class ReservationViaKakao {

        @Test
        @DisplayName("예약 정보가 완전하면 예약을 자동 생성하고 확인 메시지를 반환한다")
        fun `should auto-create reservation when info is complete`() {
            // given
            val request = webhookRequest("kakao_user_001", "내일 저녁 6시 2명 예약할게요")
            val completeResult = ReservationParseResult(
                date = LocalDate.now().plusDays(1),
                time = LocalTime.of(18, 0),
                partySize = 2,
                isComplete = true,
                missingFields = emptyList()
            )

            whenever(customerRepository.findByKakaoUserId("kakao_user_001")).thenReturn(sampleCustomer)
            whenever(customerRepository.save(any<Customer>())).thenReturn(sampleCustomer)
            whenever(storeRepository.findById(1L)).thenReturn(Optional.of(sampleStore))
            whenever(aiService.classifyIntent(any())).thenReturn(MessageType.RESERVATION)
            whenever(aiService.parseReservationInfo(any())).thenReturn(completeResult)
            whenever(aiService.generateReservationConfirmReply(any(), any()))
                .thenReturn("맛있는 카페 내일 18시 2명 예약 접수되었습니다!")
            whenever(reservationRepository.save(any<Reservation>())).thenAnswer { it.getArgument<Reservation>(0) }
            whenever(chatMessageRepository.save(any<ChatMessage>())).thenAnswer { it.getArgument<ChatMessage>(0) }

            // when
            val response = kakaoChannelService.handleMessage(1L, request)

            // then - 예약 자동 생성 확인
            verify(reservationRepository).save(argThat<Reservation> {
                storeId == 1L && partySize == 2 && memo == "카카오톡 예약 (자동)"
            })

            val text = response.template.outputs[0].simpleText?.text
            assertTrue(text!!.contains("예약 접수"))
        }

        @Test
        @DisplayName("예약 정보가 부족하면 누락 정보를 요청한다")
        fun `should ask for missing info when reservation is incomplete`() {
            // given
            val request = webhookRequest("kakao_user_001", "예약하고 싶어요")
            val incompleteResult = ReservationParseResult(
                date = null, time = null, partySize = null,
                isComplete = false,
                missingFields = listOf("날짜", "시간", "인원수")
            )

            whenever(customerRepository.findByKakaoUserId("kakao_user_001")).thenReturn(sampleCustomer)
            whenever(customerRepository.save(any<Customer>())).thenReturn(sampleCustomer)
            whenever(storeRepository.findById(1L)).thenReturn(Optional.of(sampleStore))
            whenever(aiService.classifyIntent(any())).thenReturn(MessageType.RESERVATION)
            whenever(aiService.parseReservationInfo(any())).thenReturn(incompleteResult)
            whenever(aiService.generateReservationConfirmReply(any(), any()))
                .thenReturn("예약을 도와드릴게요. 날짜, 시간, 인원수을(를) 알려주시겠어요?")
            whenever(chatMessageRepository.save(any<ChatMessage>())).thenAnswer { it.getArgument<ChatMessage>(0) }

            // when
            val response = kakaoChannelService.handleMessage(1L, request)

            // then - 예약이 생성되지 않아야 함
            verify(reservationRepository, never()).save(any())

            val text = response.template.outputs[0].simpleText?.text
            assertTrue(text!!.contains("알려주"))
        }
    }

    @Nested
    @DisplayName("카카오 스킬 응답 포맷")
    inner class ResponseFormat {

        @Test
        @DisplayName("카카오 스킬 v2.0 응답 포맷을 준수한다")
        fun `should comply with kakao skill v2 format`() {
            // given
            val request = webhookRequest("kakao_user_001", "테스트")
            whenever(customerRepository.findByKakaoUserId("kakao_user_001")).thenReturn(sampleCustomer)
            whenever(customerRepository.save(any<Customer>())).thenReturn(sampleCustomer)
            whenever(storeRepository.findById(1L)).thenReturn(Optional.of(sampleStore))
            whenever(aiService.classifyIntent(any())).thenReturn(MessageType.GENERAL)
            whenever(aiService.generateCustomerReply(any(), any(), any())).thenReturn("테스트 응답")
            whenever(chatMessageRepository.save(any<ChatMessage>())).thenAnswer { it.getArgument<ChatMessage>(0) }

            // when
            val response = kakaoChannelService.handleMessage(1L, request)

            // then
            assertEquals("2.0", response.version)
            assertEquals(1, response.template.outputs.size)
            assertNotNull(response.template.outputs[0].simpleText?.text)
        }
    }
}
