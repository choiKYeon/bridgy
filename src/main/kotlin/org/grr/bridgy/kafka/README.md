# kafka 패키지

Apache Kafka 기반 이벤트 드리븐 아키텍처 모듈입니다.
도메인 서비스에서 발생한 이벤트를 비동기로 처리하고 사장님에게 실시간 알림을 전달합니다.

## 구조

```
kafka/
├── event/
│   └── BridgyEvent.kt          # 이벤트 sealed class 계층
├── producer/
│   └── EventProducer.kt        # Kafka 이벤트 발행자
└── consumer/
    └── NotificationConsumer.kt  # 이벤트 소비자 (알림 처리)
```

## 이벤트 계층 (sealed class)

```
BridgyEvent (sealed)
├── KakaoMessageEvent      # 카카오톡 메시지 수신
├── ReservationEvent       # 예약 생성/확인/취소/완료/노쇼
├── ReviewEvent            # 리뷰 등록
└── NotificationEvent      # 사장님 알림
```

## 토픽 → 이벤트 매핑

| 토픽 | 이벤트 클래스 | 발행 시점 |
|------|--------------|-----------|
| `bridgy.kakao.message` | `KakaoMessageEvent` | 카카오톡 메시지 수신 시 (향후 비동기 처리용) |
| `bridgy.reservation.event` | `ReservationEvent` | 예약 자동 생성 시 (`KakaoChannelService`) |
| `bridgy.review.event` | `ReviewEvent` | 리뷰 등록 시 (`ReviewService`) |
| `bridgy.notification` | `NotificationEvent` | 새 문의/예약/리뷰 시 사장님 알림 |

## EventProducer

도메인 서비스에서 주입받아 사용합니다. JSON 직렬화 후 Kafka에 발행합니다.

- `publishKakaoMessage(event)` → `bridgy.kakao.message`
- `publishReservationEvent(event)` → `bridgy.reservation.event`
- `publishReviewEvent(event)` → `bridgy.review.event`
- `publishNotification(event)` → `bridgy.notification`

Key는 `storeId`를 사용하여 같은 매장의 이벤트가 같은 파티션으로 전달됩니다.

## NotificationConsumer

Consumer Group: `bridgy-notification`

3개 토픽을 구독하며, 현재는 로그 출력만 수행합니다.
향후 웹소켓, 푸시 알림, 카카오 알림톡 등으로 확장 예정입니다.

## 이벤트 발행 위치

| 서비스 | 발행 이벤트 |
|--------|------------|
| `KakaoChannelService` | `NotificationEvent`(NEW_INQUIRY/NEW_RESERVATION), `ReservationEvent`(CREATED) |
| `ReviewService` | `ReviewEvent`, `NotificationEvent`(NEW_REVIEW) |
