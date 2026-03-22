package org.grr.bridgy.kakao.service

import org.grr.bridgy.ai.service.AiService
import org.grr.bridgy.domain.chat.entity.ChatMessage
import org.grr.bridgy.domain.chat.entity.MessageSender
import org.grr.bridgy.domain.chat.entity.MessageType
import org.grr.bridgy.domain.chat.repository.ChatMessageRepository
import org.grr.bridgy.domain.customer.entity.Customer
import org.grr.bridgy.domain.customer.repository.CustomerRepository
import org.grr.bridgy.domain.reservation.entity.Reservation
import org.grr.bridgy.domain.reservation.entity.ReservationSource
import org.grr.bridgy.domain.reservation.entity.ReservationStatus
import org.grr.bridgy.domain.reservation.repository.ReservationRepository
import org.grr.bridgy.domain.store.repository.StoreRepository
import org.grr.bridgy.kafka.event.NotificationEvent
import org.grr.bridgy.kafka.event.NotificationType
import org.grr.bridgy.kafka.event.ReservationAction
import org.grr.bridgy.kafka.event.ReservationEvent
import org.grr.bridgy.kafka.producer.EventProducer
import org.grr.bridgy.kakao.dto.KakaoSkillResponse
import org.grr.bridgy.kakao.dto.KakaoWebhookRequest
import org.grr.bridgy.kakao.dto.kakaoTextResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 카카오톡 채널 메시지 처리 서비스
 *
 * 핵심 플로우:
 * 1. 고객이 카카오톡으로 메시지 전송
 * 2. 고객 자동 식별/생성 (회원가입 불필요)
 * 3. 메시지 의도 분류 (일반/예약/영업정보)
 * 4. AI 자동 응답 생성
 * 5. 대화 내역 DB 저장 (사장님 대시보드에서 확인 가능)
 * 6. 예약 의도면 자연어 파싱 → 예약 자동 생성
 */
@Service
@Transactional
class KakaoChannelService(
    private val customerRepository: CustomerRepository,
    private val storeRepository: StoreRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val reservationRepository: ReservationRepository,
    private val aiService: AiService,
    private val eventProducer: EventProducer
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun handleMessage(storeId: Long, request: KakaoWebhookRequest): KakaoSkillResponse {
        val kakaoUserId = request.userRequest.user.id
        val message = request.userRequest.utterance

        log.info("[카카오] 메시지 수신 - storeId: $storeId, user: $kakaoUserId, message: $message")

        // 1. 고객 자동 식별/생성
        val customer = getOrCreateCustomer(kakaoUserId)

        // 2. 매장 조회
        val store = storeRepository.findById(storeId).orElse(null)
            ?: return kakaoTextResponse("죄송합니다. 매장 정보를 찾을 수 없습니다.")

        // 3. 메시지 의도 분류
        val messageType = aiService.classifyIntent(message)

        // 4. 고객 메시지 저장
        saveChatMessage(storeId, kakaoUserId, MessageSender.CUSTOMER, message, messageType)

        // 5. 의도별 처리 + AI 응답 생성
        val reply = when (messageType) {
            MessageType.RESERVATION -> handleReservation(storeId, kakaoUserId, store.name, message)
            else -> aiService.generateCustomerReply(
                storeName = store.name,
                storeDescription = store.description,
                customerMessage = message
            )
        }

        // 6. AI 응답 저장
        saveChatMessage(storeId, kakaoUserId, MessageSender.AI, reply, messageType)

        // 7. 새 문의 알림 이벤트 발행 (사장님 대시보드 알림)
        eventProducer.publishNotification(
            NotificationEvent(
                storeId = storeId,
                ownerEmail = store.ownerEmail,
                title = "새로운 카카오톡 문의",
                message = "고객 메시지: ${message.take(50)}",
                notificationType = if (messageType == MessageType.RESERVATION)
                    NotificationType.NEW_RESERVATION else NotificationType.NEW_INQUIRY
            )
        )

        log.info("[카카오] 응답 전송 - storeId: $storeId, reply: $reply")
        return kakaoTextResponse(reply)
    }

    /**
     * 카카오 유저 ID로 고객 조회, 없으면 자동 생성
     */
    private fun getOrCreateCustomer(kakaoUserId: String): Customer {
        val existing = customerRepository.findByKakaoUserId(kakaoUserId)
        if (existing != null) {
            existing.touch()
            return customerRepository.save(existing)
        }
        return customerRepository.save(Customer(kakaoUserId = kakaoUserId))
    }

    /**
     * 예약 의도 메시지 처리
     * 자연어에서 날짜/시간/인원을 파싱하여 예약 생성 시도
     */
    private fun handleReservation(
        storeId: Long,
        kakaoUserId: String,
        storeName: String,
        message: String
    ): String {
        val parseResult = aiService.parseReservationInfo(message)

        if (parseResult.isComplete) {
            // 예약 정보가 모두 갖춰지면 예약 자동 생성
            val customer = customerRepository.findByKakaoUserId(kakaoUserId)!!
            val reservation = Reservation(
                storeId = storeId,
                customerId = customer.id,
                reservationDate = parseResult.date!!,
                reservationTime = parseResult.time!!,
                partySize = parseResult.partySize!!,
                memo = "카카오톡 예약 (자동)",
                status = ReservationStatus.PENDING,
                source = ReservationSource.KAKAOTALK
            )
            val savedReservation = reservationRepository.save(reservation)

            // 예약 생성 이벤트 발행
            eventProducer.publishReservationEvent(
                ReservationEvent(
                    storeId = storeId,
                    reservationId = savedReservation.id,
                    customerId = customer.id,
                    action = ReservationAction.CREATED,
                    summary = "${parseResult.date} ${parseResult.time} ${parseResult.partySize}명 예약"
                )
            )

            log.info("[예약] 자동 생성 - storeId: $storeId, date: ${parseResult.date}, time: ${parseResult.time}, partySize: ${parseResult.partySize}")
        }

        return aiService.generateReservationConfirmReply(storeName, parseResult)
    }

    private fun saveChatMessage(
        storeId: Long,
        kakaoUserId: String,
        sender: MessageSender,
        content: String,
        messageType: MessageType
    ) {
        chatMessageRepository.save(
            ChatMessage(
                storeId = storeId,
                kakaoUserId = kakaoUserId,
                sender = sender,
                content = content,
                messageType = messageType
            )
        )
    }
}
