# customer 패키지

카카오톡으로 문의한 고객을 관리하는 도메인입니다.

## 구조

```
customer/
├── entity/
│   └── Customer.kt             # 카카오 유저 엔티티 (kakao_users 테이블)
└── repository/
    └── CustomerRepository.kt   # JPA Repository
```

## 핵심 개념

고객은 **회원가입 없이** 카카오톡 유저 ID로 자동 식별/생성됩니다.

### Customer 엔티티

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | Long | PK (auto increment) |
| `kakaoUserId` | String | 카카오 유저 고유 ID (unique) |
| `nickname` | String? | 닉네임 (선택) |
| `firstContactAt` | LocalDateTime | 최초 문의 시각 |
| `lastContactAt` | LocalDateTime | 최근 문의 시각 |

### 주요 메서드

- `touch()`: 문의 시 `lastContactAt`을 현재 시각으로 갱신

### Repository

- `findByKakaoUserId(kakaoUserId)`: 카카오 유저 ID로 고객 조회
