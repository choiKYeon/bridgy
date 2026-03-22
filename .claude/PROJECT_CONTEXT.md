# Bridgy 프로젝트 컨텍스트

> 이 파일은 AI 어시스턴트가 프로젝트를 빠르게 파악하기 위한 레퍼런스입니다.
> 새 세션 시작 시 이 파일을 먼저 읽으면 프로젝트 전체 맥락을 이해할 수 있습니다.

## 한줄 요약

소상공인 매장 ↔ 고객을 카카오톡으로 연결하는 AI 에이전트 플랫폼 (Kotlin + Spring Boot)

## 핵심 철학

- **고객 진입 장벽 제로**: 앱 설치, 회원가입 없음. 카카오톡으로만 소통.
- **AI 자동화**: 고객 문의 자동 응답, 예약 자연어 파싱, 리뷰 자동 답글
- **사장님 대시보드**: 웹에서 매장/예약/대화/리뷰 관리
- **이벤트 드리븐**: Kafka로 비동기 알림, Redis로 캐싱

---

## 기술 스택

| 구분 | 기술 | 비고 |
|------|------|------|
| Language | Kotlin 2.0.0 | |
| Framework | Spring Boot 3.2.5 | JDK 17 필수 |
| DB | PostgreSQL 16 | 운영 |
| DB (로컬) | H2 in-memory | 로컬/테스트 |
| Cache | Redis 7 | @Cacheable, TTL 캐시별 |
| MQ | Apache Kafka | Confluent 7.6.0, 4개 토픽 |
| ORM | Spring Data JPA | |
| API Docs | SpringDoc OpenAPI 2.5.0 | |
| Test | JUnit 5 + Mockito-Kotlin 5.3.1 | 82개 테스트 |
| Infra | Docker Compose | PG + Redis + Kafka + ZK + Kafka UI |

---

## 프로젝트 구조

```
src/main/kotlin/org/grr/bridgy/
├── ai/service/AiService.kt           # 의도 분류, NLP 예약 파싱, 응답 생성 (현재 Mock)
├── config/
│   ├── KafkaConfig.kt                # 4개 토픽 정의 (3파티션/1레플리카)
│   ├── RedisConfig.kt                # 캐시 TTL: store(30m), storesByOwner(15m), reservations(5m), reviews(10m)
│   ├── SecurityConfig.kt             # CSRF off, stateless, 카카오 웹훅 permitAll, TODO: JWT
│   └── WebConfig.kt                  # CORS: localhost:3000, 5173
├── domain/
│   ├── chat/                          # 대화 내역 (ChatMessage 엔티티, 대시보드 조회 API)
│   ├── customer/                      # 카카오 유저 (kakao_users 테이블, 자동 생성)
│   ├── reservation/                   # 예약 (PENDING→CONFIRMED→COMPLETED/CANCELLED/NO_SHOW)
│   │                                  #   source: KAKAOTALK / DASHBOARD / PHONE
│   ├── review/                        # 리뷰 + AI 자동 답글 + 사장님 답글
│   └── store/                         # 매장 CRUD + Redis @Cacheable
├── kafka/
│   ├── event/BridgyEvent.kt          # sealed class 이벤트 계층
│   ├── producer/EventProducer.kt     # JSON 직렬화 → Kafka 발행
│   └── consumer/NotificationConsumer.kt  # 이벤트 소비 (현재 로그, TODO: 실제 알림)
└── kakao/
    ├── controller/KakaoWebhookController.kt  # POST /api/kakao/webhook/{storeId}
    ├── dto/KakaoDto.kt                       # 카카오 스킬 v2.0 포맷
    └── service/KakaoChannelService.kt        # 핵심 플로우 (아래 참조)
```

---

## 핵심 플로우: 카카오톡 메시지 처리

```
카카오톡 웹훅 → KakaoWebhookController
  → KakaoChannelService.handleMessage()
    1. 고객 자동 식별/생성 (kakaoUserId)
    2. 매장 조회 (없으면 에러 응답)
    3. AI 의도 분류 (GENERAL / RESERVATION / REVIEW / BUSINESS_INFO)
    4. 고객 메시지 DB 저장
    5. 의도별 처리:
       - RESERVATION → 자연어 파싱 → 예약 자동 생성 → ReservationEvent 발행
       - 그 외 → AI 응답 생성
    6. AI 응답 DB 저장
    7. NotificationEvent 발행 (사장님 알림)
    8. 카카오 스킬 v2.0 응답 반환
```

## 자연어 예약 파싱 (AiService)

입력: "내일 저녁 6시 2명 예약할게요"
→ date: 내일 날짜, time: 18:00, partySize: 2

지원 패턴:
- 날짜: "오늘", "내일", "모레", "4월 5일", "4/5"
- 시간: "6시", "6시반", "오후 3시 30분" (오후/저녁 12시간 보정)
- 인원: "2명", "4인", "3분"
- 부족한 정보는 missingFields로 재질문

---

## API 엔드포인트 요약

### 카카오 웹훅
- `POST /api/kakao/webhook/{storeId}` — 메시지 수신
- `GET /api/kakao/webhook/health` — 헬스체크

### 대시보드 (사장님)
- `GET /api/dashboard/stores/{storeId}/chats` — 대화 내역
- `GET /api/dashboard/stores/{storeId}/chats/users/{kakaoUserId}` — 고객별 대화
- `GET /api/dashboard/stores/{storeId}/chats/threads` — 스레드 요약
- `GET /api/dashboard/stores/{storeId}/chats/reservations` — 예약 대화
- `GET /api/dashboard/stores/{storeId}/reservations` — 예약 목록
- `PUT .../reservations/{id}/confirm|cancel|complete|no-show` — 예약 상태 변경

### 매장
- `POST /api/stores` | `GET /api/stores/{id}` | `PUT /api/stores/{id}` | `DELETE /api/stores/{id}`
- `GET /api/stores/owner/{ownerEmail}`

### 리뷰
- `GET /api/reviews/store/{storeId}` | `POST /api/reviews` | `POST /api/reviews/{id}/reply`

---

## Kafka 토픽

| 토픽 | 발행 위치 | 용도 |
|------|-----------|------|
| `bridgy.kakao.message` | (향후) | 비동기 메시지 처리 |
| `bridgy.reservation.event` | KakaoChannelService | 예약 생성 이벤트 |
| `bridgy.review.event` | ReviewService | 리뷰 등록 이벤트 |
| `bridgy.notification` | KakaoChannelService, ReviewService | 사장님 알림 |

---

## 테스트 현황 (82개)

| 파일 | 수 | 방식 |
|------|----|------|
| AiServiceTest | 20 | 단위 |
| KakaoChannelServiceTest | 7 | 단위 (Mock) |
| ChatServiceTest | 5 | 단위 |
| ChatControllerTest | 4 | @WebMvcTest |
| StoreServiceTest | 9 | 단위 |
| StoreControllerTest | 6 | @WebMvcTest |
| ReservationServiceTest | 8 | 단위 |
| ReservationControllerTest | 5 | @WebMvcTest |
| ReviewServiceTest | 7 | 단위 |
| ReviewControllerTest | 6 | @WebMvcTest |
| KakaoWebhookControllerTest | 5 | @WebMvcTest |

테스트 환경: `src/test/resources/application.yml`에서 Kafka 자동설정 제외, Redis 캐시 비활성화

---

## 설정 파일 관리

- `application.yml`, `application-*.yml`은 `.gitignore`에 등록 (민감 정보)
- `.example` 파일 제공: `application.yml.example`, `application-local.yml.example`
- 신규 개발자: `.example`을 복사하여 사용

---

## 로컬 실행

```bash
docker-compose up -d                                          # 인프라
cp src/main/resources/application.yml.example src/main/resources/application.yml
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
./gradlew bootRun --args='--spring.profiles.active=local'     # 앱 실행
```

포트: 앱(8080), PostgreSQL(5432), Redis(6379), Kafka(9092), Kafka UI(8090)

---

## GitHub

`https://github.com/choiKYeon/bridgy`

---

## 향후 계획 (TODO)

- [ ] 실제 AI API 연동 (OpenAI / Claude) — 현재 Mock 구현
- [ ] 카카오 i 오픈빌더 실제 채널 연동
- [ ] 사장님 웹 대시보드 프론트엔드 (React)
- [ ] Kafka Consumer 실제 알림 구현 (이메일, 푸시, 카카오 알림톡)
- [ ] Spring Security JWT 인증
- [ ] ReservationService, ReviewService에 @Cacheable 추가
- [ ] CI/CD 배포 파이프라인
- [ ] KakaoMessageEvent를 활용한 비동기 메시지 처리

---

## 주의사항

- Spring Boot 3.x → **JDK 17 필수** (JDK 11 안 됨)
- `build.gradle`에 `kotlin-jpa` 플러그인 필수 (JPA 엔티티 no-arg constructor)
- ChatMessageRepository의 `countDistinctCustomersByStoreId`는 JPQL `@Query` 사용 (JPA 파생 쿼리 불가)
- Reservation 엔티티에 `source` 필드 추가됨 — 생성 시 반드시 전달
