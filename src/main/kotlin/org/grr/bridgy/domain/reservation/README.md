# reservation 패키지

예약 관리 도메인입니다. 카카오톡 자연어 예약과 사장님 대시보드 예약 관리를 담당합니다.

## 구조

```
reservation/
├── controller/
│   └── ReservationController.kt    # 대시보드 예약 관리 API
├── dto/
│   └── ReservationDto.kt           # 요청/응답 DTO
├── entity/
│   └── Reservation.kt              # 예약 엔티티
├── repository/
│   └── ReservationRepository.kt    # JPA Repository
└── service/
    └── ReservationService.kt       # 예약 비즈니스 로직
```

## Reservation 엔티티

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | Long | PK |
| `storeId` | Long | 매장 ID |
| `customerId` | Long | 고객 ID |
| `reservationDate` | LocalDate | 예약 날짜 |
| `reservationTime` | LocalTime | 예약 시간 |
| `partySize` | Int | 인원수 |
| `memo` | String? | 메모 |
| `status` | ReservationStatus | 예약 상태 |
| `source` | ReservationSource | 예약 출처 |
| `createdAt` | LocalDateTime | 생성 시각 |

### ReservationStatus

`PENDING` → `CONFIRMED` → `COMPLETED` 또는 `CANCELLED` / `NO_SHOW`

### ReservationSource

| 값 | 설명 |
|----|------|
| `KAKAOTALK` | 카카오톡 자연어 예약 (자동 생성) |
| `DASHBOARD` | 사장님 대시보드에서 직접 생성 |
| `PHONE` | 전화 예약 (수동 입력) |

## API 엔드포인트

`/api/dashboard/stores/{storeId}/reservations` 하위:

| Method | Path | 설명 |
|--------|------|------|
| GET | `/` | 매장 예약 목록 |
| PUT | `/{id}/confirm` | 예약 확인 |
| PUT | `/{id}/cancel` | 예약 취소 |
| PUT | `/{id}/complete` | 방문 완료 처리 |
| PUT | `/{id}/no-show` | 노쇼 처리 |

## 관련 테스트

- `ReservationServiceTest.kt` (8개): 상태 변경, 취소, 생성 검증
- `ReservationControllerTest.kt` (5개): API 엔드포인트 테스트
