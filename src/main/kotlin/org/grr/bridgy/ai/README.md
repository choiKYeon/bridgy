# ai 패키지

AI 기반 자동 응답 및 자연어 처리 모듈입니다.

## 구조

```
ai/
└── service/
    └── AiService.kt          # AI 서비스 (의도 분류, NLP 파싱, 응답 생성)
```

## 핵심 기능

### 1. 메시지 의도 분류 (`classifyIntent`)

고객 메시지를 4가지 의도로 분류합니다.

| 의도 | 키워드 예시 |
|------|-------------|
| `RESERVATION` | 예약, 자리, 테이블, 몇 명 |
| `BUSINESS_INFO` | 영업시간, 메뉴, 가격, 주차, 위치 |
| `REVIEW` | 리뷰, 후기 |
| `GENERAL` | 그 외 모든 메시지 |

### 2. 예약 정보 자연어 파싱 (`parseReservationInfo`)

한국어 자연어에서 날짜, 시간, 인원수를 추출합니다.

- **날짜**: "오늘", "내일", "모레", "4월 5일", "4/5"
- **시간**: "6시", "6시반", "오후 3시 30분" (오후/저녁 보정 포함)
- **인원**: "2명", "4인", "3분"

파싱 결과는 `ReservationParseResult`로 반환되며, 정보가 부족하면 `missingFields`에 누락 항목이 담깁니다.

### 3. 응답 생성

- `generateCustomerReply()`: 고객 문의에 대한 AI 응답
- `generateReservationConfirmReply()`: 예약 확인/추가 정보 요청 응답
- `generateReviewReply()`: 리뷰 자동 답글 (평점 기반 톤 조절)

## 현재 상태

현재는 **Mock 구현**으로 키워드 기반 규칙형 응답을 생성합니다.
`ai.provider` 설정값이 `mock`이 아닌 경우 실제 AI API를 호출하도록 확장 예정입니다.

## 관련 테스트

- `AiServiceTest.kt` (20개 테스트): 의도 분류, 예약 파싱, 응답 생성 검증
