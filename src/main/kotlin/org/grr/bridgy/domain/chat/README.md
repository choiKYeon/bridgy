# chat 패키지

카카오톡 대화 내역을 저장하고 사장님 대시보드에서 조회하는 도메인입니다.

## 구조

```
chat/
├── controller/
│   └── ChatController.kt           # 대시보드 대화 조회 API
├── dto/
│   └── ChatDto.kt                  # 응답 DTO
├── entity/
│   └── ChatMessage.kt              # 대화 메시지 엔티티
├── repository/
│   └── ChatMessageRepository.kt    # JPA Repository + 커스텀 쿼리
└── service/
    └── ChatService.kt              # 대화 조회 비즈니스 로직
```

## ChatMessage 엔티티

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | Long | PK |
| `storeId` | Long | 매장 ID |
| `kakaoUserId` | String | 카카오 유저 ID |
| `sender` | MessageSender | 발신자 (CUSTOMER / AI / OWNER) |
| `content` | String | 메시지 내용 |
| `messageType` | MessageType | 의도 (GENERAL / RESERVATION / REVIEW / BUSINESS_INFO) |
| `createdAt` | LocalDateTime | 생성 시각 |

인덱스: `(store_id, kakao_user_id, created_at)` 복합 인덱스

## API 엔드포인트

모든 엔드포인트는 `/api/dashboard/stores/{storeId}/chats` 하위입니다.

| Method | Path | 설명 |
|--------|------|------|
| GET | `/` | 매장 전체 대화 내역 (페이징) |
| GET | `/users/{kakaoUserId}` | 특정 고객과의 대화 |
| GET | `/threads` | 고객별 대화 스레드 요약 |
| GET | `/reservations` | 예약 관련 대화만 필터링 |

## 관련 테스트

- `ChatServiceTest.kt` (5개): 페이징, 고객별 조회, 스레드 요약, 예약 필터링
- `ChatControllerTest.kt` (4개): API 엔드포인트 테스트
