# Bridgy

사람들의 반려동물을 소개하고 공유하는 소셜 플랫폼입니다.

반려동물 프로필 등록, 사진 갤러리, 인기 반려동물 대시보드까지 한 곳에서 관리하고, 다른 반려인들과 소통할 수 있습니다. 한 사용자가 여러 마리의 반려동물을 등록할 수 있습니다.

## 기술 스택

- **Language**: Kotlin 2.0.0
- **Framework**: Spring Boot 3.2.5 (JDK 17)
- **Database**: PostgreSQL 16 (운영) / H2 (로컬/테스트)
- **Cache**: Redis 7
- **ORM**: Spring Data JPA
- **API Docs**: SpringDoc OpenAPI 2.5.0
- **Test**: JUnit 5 + Mockito-Kotlin
- **Infra**: Docker Compose
- **Frontend**: Flutter (별도 프로젝트)

## 프로젝트 구조

```
src/main/kotlin/org/grr/bridgy/
├── config/             # Redis, Security, CORS 설정
└── domain/
    ├── user/           # 사용자 (회원가입, 로그인, 프로필)
    ├── pet/            # 반려동물 프로필 + 대시보드 (인기순/최신순 피드)
    ├── gallery/        # 사진 갤러리
    ├── comment/        # 댓글
    └── like/           # 좋아요
```

## 주요 기능

### 사용자 관리
- 회원가입, 로그인, 프로필 수정

### 반려동물 프로필
- 반려동물 등록 (이름, 종류, 품종, 나이, 성별, 체중, 소개)
- **한 사용자가 여러 마리 등록 가능**
- 사용자별 반려동물 목록 조회
- 이름/종류별 검색

### 대시보드 (인기 반려동물)
- **인기순 피드**: 좋아요 많은 순으로 반려동물 조회
- **최신순 피드**: 최근 등록된 반려동물 조회
- 각 반려동물의 좋아요 수, 댓글 수, 갤러리 수 포함
- 현재 사용자의 좋아요 여부 표시

### 사진 갤러리
- 반려동물별 사진 업로드 및 캡션 작성
- 갤러리 조회

### 소셜 기능
- 반려동물 프로필에 좋아요 (토글)
- 댓글 작성 및 소통

## API 엔드포인트

| 도메인 | 경로 | 설명 |
|--------|------|------|
| 사용자 | `POST /api/users/signup` | 회원가입 |
| 사용자 | `GET /api/users/{id}` | 사용자 조회 |
| 반려동물 | `POST /api/pets` | 반려동물 등록 |
| 반려동물 | `GET /api/pets/user/{userId}` | 내 반려동물 목록 (다중 펫) |
| 반려동물 | `GET /api/pets/search?name=` | 이름 검색 |
| **대시보드** | `GET /api/pets/feed/popular` | **인기순 피드** |
| **대시보드** | `GET /api/pets/feed/recent` | **최신순 피드** |
| 상세 | `GET /api/pets/{id}/detail` | 반려동물 상세 (좋아요/댓글수 포함) |
| 갤러리 | `POST /api/gallery` | 사진 업로드 |
| 갤러리 | `GET /api/gallery/pet/{petId}` | 사진 조회 |
| 댓글 | `POST /api/comments` | 댓글 작성 |
| 댓글 | `GET /api/comments/pet/{petId}` | 댓글 조회 |
| 좋아요 | `POST /api/likes` | 좋아요 토글 |
| 좋아요 | `GET /api/likes/pet/{petId}/user/{userId}` | 좋아요 상태 |

## 시작하기

### 1. 인프라 실행

```bash
docker-compose up -d
```

PostgreSQL(5432), Redis(6379)가 실행됩니다.

### 2. 애플리케이션 실행

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

### 3. API 문서 확인

```
http://localhost:8080/swagger-ui/index.html
```

## 테스트

```bash
./gradlew test
```

## GitHub

```
https://github.com/choiKYeon/bridgy
```
