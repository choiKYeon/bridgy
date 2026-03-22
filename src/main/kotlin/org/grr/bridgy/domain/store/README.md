# store 패키지

매장 관리 도메인입니다. 사장님이 대시보드에서 매장 정보를 등록/관리합니다.

## 구조

```
store/
├── controller/
│   └── StoreController.kt     # 매장 CRUD API
├── dto/
│   └── StoreDto.kt            # 요청/응답 DTO
├── entity/
│   └── Store.kt               # 매장 엔티티
├── repository/
│   └── StoreRepository.kt     # JPA Repository
└── service/
    └── StoreService.kt        # 매장 비즈니스 로직 + Redis 캐싱
```

## Store 엔티티 주요 필드

| 필드 | 타입 | 설명 |
|------|------|------|
| `name` | String | 매장명 |
| `category` | String | 업종 (카페, 음식점 등) |
| `address` | String | 주소 |
| `phone` | String | 전화번호 |
| `description` | String? | 매장 소개 |
| `openTime` / `closeTime` | LocalTime? | 영업시간 |
| `closedDays` | List<String> | 휴무일 |
| `kakaoChannelId` | String? | 카카오톡 채널 ID |
| `ownerEmail` | String | 사장님 이메일 |

## Redis 캐싱

| 캐시 키 | 어노테이션 | TTL |
|---------|-----------|-----|
| `store` | `@Cacheable` on `getStore()` | 30분 |
| `storesByOwner` | `@Cacheable` on `getStoresByOwner()` | 15분 |

매장 생성/수정/삭제 시 `@CacheEvict`로 캐시를 자동 무효화합니다.

## API 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/stores` | 매장 등록 |
| GET | `/api/stores/{id}` | 매장 조회 |
| GET | `/api/stores/owner/{ownerEmail}` | 사장님별 매장 목록 |
| PUT | `/api/stores/{id}` | 매장 수정 |
| DELETE | `/api/stores/{id}` | 매장 삭제 |

## 관련 테스트

- `StoreServiceTest.kt` (9개): CRUD 및 캐시 동작
- `StoreControllerTest.kt` (6개): API 엔드포인트 테스트
