# review 패키지

리뷰 관리 + AI 자동 답글 도메인입니다.

## 구조

```
review/
├── controller/
│   └── ReviewController.kt     # 리뷰 API
├── dto/
│   └── ReviewDto.kt            # 요청/응답 DTO
├── entity/
│   └── Review.kt               # 리뷰 엔티티
├── repository/
│   └── ReviewRepository.kt     # JPA Repository
└── service/
    └── ReviewService.kt        # 리뷰 비즈니스 로직 + Kafka 이벤트 발행
```

## 핵심 플로우

1. 고객이 리뷰 등록
2. AI가 평점 기반 자동 답글 생성 (`AiService.generateReviewReply`)
3. Kafka `ReviewEvent` 발행
4. 사장님에게 `NotificationEvent`(NEW_REVIEW) 알림 발행
5. 사장님이 별도로 직접 답글 작성 가능 (`addOwnerReply`)

## Review 엔티티 주요 필드

| 필드 | 타입 | 설명 |
|------|------|------|
| `storeId` | Long | 매장 ID |
| `customerId` | Long | 고객 ID |
| `rating` | Int | 평점 (1~5) |
| `content` | String | 리뷰 내용 |
| `aiReply` | String? | AI 자동 답글 |
| `ownerReply` | String? | 사장님 직접 답글 |
| `repliedAt` | LocalDateTime? | 답글 시각 |

## API 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/reviews/store/{storeId}` | 매장 리뷰 목록 |
| POST | `/api/reviews` | 리뷰 등록 (AI 자동 답글 포함) |
| POST | `/api/reviews/{id}/reply` | 사장님 답글 등록 |

## 관련 테스트

- `ReviewServiceTest.kt` (7개): 리뷰 생성, AI 답글, 사장님 답글
- `ReviewControllerTest.kt` (6개): API 엔드포인트 테스트
