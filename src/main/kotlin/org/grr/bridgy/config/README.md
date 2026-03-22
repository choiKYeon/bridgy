# config 패키지

Spring Boot 설정 클래스 모음입니다.

## 구조

```
config/
├── KafkaConfig.kt      # Kafka 토픽 정의 및 생성
├── RedisConfig.kt      # Redis 캐시 매니저 + 직렬화 설정
├── SecurityConfig.kt   # Spring Security 설정
└── WebConfig.kt        # CORS 설정
```

## KafkaConfig

4개의 Kafka 토픽을 정의하고 자동 생성합니다. 각 토픽은 파티션 3개, 레플리카 1개입니다.

| 상수 | 토픽명 | 용도 |
|------|--------|------|
| `TOPIC_KAKAO_MESSAGE` | `bridgy.kakao.message` | 카카오톡 메시지 비동기 처리 |
| `TOPIC_RESERVATION_EVENT` | `bridgy.reservation.event` | 예약 생성/상태변경 이벤트 |
| `TOPIC_REVIEW_EVENT` | `bridgy.review.event` | 리뷰 등록 이벤트 |
| `TOPIC_NOTIFICATION` | `bridgy.notification` | 사장님 실시간 알림 |

## RedisConfig

- `@EnableCaching`으로 Spring Cache 활성화
- JSON 직렬화 (`GenericJackson2JsonRedisSerializer`) + `JavaTimeModule` 등록
- 캐시별 TTL: store(30분), storesByOwner(15분), reservations(5분), reviews(10분)
- null 값 캐싱 비활성화

## SecurityConfig

- CSRF 비활성화 (REST API 서버)
- Stateless 세션 정책
- `/api/kakao/**`: 인증 없이 허용 (카카오 웹훅)
- `/swagger-ui/**`, `/v3/api-docs/**`: Swagger UI 허용
- `/h2-console/**`: 로컬 개발용 허용
- 나머지: 현재 `permitAll()` → 추후 JWT 인증 적용 예정

## WebConfig

- CORS: `localhost:3000`, `localhost:5173` (프론트엔드 개발 서버) 허용
- 허용 메서드: GET, POST, PUT, PATCH, DELETE, OPTIONS
