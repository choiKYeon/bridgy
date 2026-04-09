# Bridgy API 문서

사람들의 반려동물을 소개하고 공유하는 소셜 플랫폼입니다.

반려동물 프로필 등록, 사진 갤러리, 인기 반려동물 대시보드까지 한 곳에서 관리하고, 다른 반려인들과 소통할 수 있습니다.

**이 문서는 프론트엔드 개발자가 Bridgy API와 통합하기 위한 가이드입니다.**

---

## 📋 목차

1. [기술 스택](#기술-스택)
2. [빠른 시작](#빠른-시작)
3. [인증 (Authentication)](#인증-authentication)
4. [API 구조](#api-구조)
5. [엔드포인트 정리](#엔드포인트-정리)
6. [응답 구조 및 DTO](#응답-구조-및-dto)
7. [자유 플랜 제한 사항](#자유-플랜-제한-사항)
8. [에러 처리](#에러-처리)
9. [페이지네이션](#페이지네이션)
10. [사용 예시](#사용-예시)
11. [프로젝트 구조](#프로젝트-구조)

---

## 기술 스택

**백엔드**
- Language: Kotlin 2.0.0
- Framework: Spring Boot 3.2.5 (JDK 17)
- Database: PostgreSQL 16 (운영) / H2 (로컬/테스트)
- Cache: Redis 7
- ORM: Spring Data JPA
- API Docs: SpringDoc OpenAPI 2.5.0

**테스트 & 배포**
- Test: JUnit 5 + Mockito-Kotlin
- Infra: Docker Compose

**프론트엔드**
- Flutter (별도 프로젝트)

**⚠️ 중요: JSON 필드명**
- 모든 JSON 필드는 **SNAKE_CASE** 입니다.
- 예: `userId` → `user_id`, `createdAt` → `created_at`

---

## 빠른 시작

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

Swagger UI를 통해 대화형 API 문서를 확인할 수 있습니다.

```
http://localhost:8080/swagger-ui/index.html
```

### 4. 테스트 실행

```bash
./gradlew test
```

---

## 인증 (Authentication)

### 🔐 인증 흐름

Bridgy API는 JWT(JSON Web Token) 기반 인증을 사용합니다.

#### 1단계: 로그인

이메일과 비밀번호로 로그인하여 토큰을 받습니다.

```http
POST /api/v0/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

**응답 (200 OK)**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer"
}
```

#### 2단계: 인증된 요청 (Access Token 사용)

모든 V1 엔드포인트 요청 시 **Authorization 헤더**에 Access Token을 포함합니다.

```http
GET /api/v1/pets
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

#### 3단계: 토큰 갱신

Access Token이 만료되면 Refresh Token을 사용하여 새로운 토큰을 받습니다.

```http
POST /api/v0/auth/refresh
Content-Type: application/json

{
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**응답 (200 OK)**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer"
}
```

#### 4단계: 로그아웃

로그아웃 시 Refresh Token을 무효화합니다.

```http
POST /api/v1/auth/logout
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**응답 (204 No Content)**

---

## API 구조

### 엔드포인트 분류

Bridgy API는 두 가지 버전의 엔드포인트를 제공합니다.

| 버전 | 경로 패턴 | 인증 필요 | 설명 |
|------|---------|---------|------|
| **V0** | `/api/v0/**` | ❌ 불필요 | 공개 데이터 조회 (로그인, 반려동물 검색 등) |
| **V1** | `/api/v1/**` | ✅ 필수 | 개인 작업 (반려동물 등록, 댓글 작성 등) |

---

## 엔드포인트 정리

### 🔑 인증 (Auth)

| 메서드 | 경로 | 설명 | 인증 필요 |
|--------|------|------|---------|
| `POST` | `/api/v0/auth/login` | 로그인 | ❌ |
| `POST` | `/api/v0/auth/refresh` | 토큰 갱신 | ❌ |
| `POST` | `/api/v1/auth/logout` | 로그아웃 | ✅ |

### 👤 사용자 (User)

| 메서드 | 경로 | 설명 | 인증 필요 |
|--------|------|------|---------|
| `POST` | `/api/v0/auth/signup` | 회원가입 | ❌ |
| `GET` | `/api/v1/users/{userId}` | 사용자 조회 | ✅ |
| `PUT` | `/api/v1/users/{userId}` | 사용자 수정 | ✅ |
| `DELETE` | `/api/v1/users/{userId}` | 사용자 삭제 | ✅ |

### 🐾 반려동물 (Pet)

| 메서드 | 경로 | 설명 | 인증 필요 | 비고 |
|--------|------|------|---------|------|
| `GET` | `/api/v0/pets/{petId}` | 반려동물 조회 | ❌ | - |
| `GET` | `/api/v0/pets` | 모든 반려동물 조회 | ❌ | - |
| `GET` | `/api/v0/pets/user/{userId}` | 사용자의 반려동물 조회 | ❌ | - |
| `GET` | `/api/v0/pets/search?name=&page=&size=` | 반려동물 검색 | ❌ | 이름 기준 검색 |
| `GET` | `/api/v0/pets/species/{species}` | 종류별 조회 | ❌ | - |
| `GET` | `/api/v0/pets/feed/popular?userId=&page=&size=` | 인기순 피드 | ❌ | 좋아요 많은 순 |
| `GET` | `/api/v0/pets/feed/recent?userId=&page=&size=` | 최신순 피드 | ❌ | 최근 등록순 |
| `GET` | `/api/v0/pets/{petId}/detail?userId=` | 반려동물 상세 | ❌ | 좋아요/댓글 수 포함 |
| `POST` | `/api/v1/pets` | 반려동물 등록 | ✅ | ⚠️ 최대 3마리 |
| `PUT` | `/api/v1/pets/{petId}` | 반려동물 수정 | ✅ | - |
| `DELETE` | `/api/v1/pets/{petId}` | 반려동물 삭제 | ✅ | - |

### 📷 갤러리 (Gallery)

| 메서드 | 경로 | 설명 | 인증 필요 | 비고 |
|--------|------|------|---------|------|
| `GET` | `/api/v0/gallery/pet/{petId}` | 반려동물 사진 조회 | ❌ | - |
| `GET` | `/api/v0/gallery/{photoId}` | 특정 사진 조회 | ❌ | - |
| `POST` | `/api/v1/gallery` | 사진 업로드 | ✅ | ⚠️ 반려동물당 최대 20장 |
| `DELETE` | `/api/v1/gallery/{photoId}` | 사진 삭제 | ✅ | - |

### 💬 댓글 (Comment)

| 메서드 | 경로 | 설명 | 인증 필요 | 비고 |
|--------|------|------|---------|------|
| `GET` | `/api/v0/comments/pet/{petId}` | 반려동물 댓글 조회 | ❌ | - |
| `GET` | `/api/v0/comments/pet/{petId}/count` | 댓글 수 조회 | ❌ | - |
| `POST` | `/api/v1/comments` | 댓글 작성 | ✅ | ⚠️ 일일 최대 30개, 최대 300자 |
| `PUT` | `/api/v1/comments/{commentId}` | 댓글 수정 | ✅ | ⚠️ 최대 300자 |
| `DELETE` | `/api/v1/comments/{commentId}` | 댓글 삭제 | ✅ | - |

### 👍 좋아요 (Like)

| 메서드 | 경로 | 설명 | 인증 필요 | 비고 |
|--------|------|------|---------|------|
| `GET` | `/api/v0/likes/pet/{petId}/user/{userId}` | 좋아요 상태 조회 | ❌ | - |
| `GET` | `/api/v0/likes/pet/{petId}/count` | 좋아요 수 조회 | ❌ | - |
| `POST` | `/api/v1/likes` | 좋아요 토글 | ✅ | - |

### 🎨 장식 (Decoration)

| 메서드 | 경로 | 설명 | 인증 필요 | 비고 |
|--------|------|------|---------|------|
| `GET` | `/api/v0/decorations` | 모든 장식 조회 | ❌ | - |
| `GET` | `/api/v0/decorations/type/{type}` | 유형별 장식 조회 | ❌ | BORDER / BADGE |
| `GET` | `/api/v0/decorations/basic` | 기본 장식 조회 | ❌ | 무료 티어 |
| `GET` | `/api/v0/decorations/pet/{petId}` | 반려동물 장식 조회 | ❌ | - |
| `POST` | `/api/v1/decorations/equip` | 장식 적용 | ✅ | ⚠️ BASIC 티어만 가능 |
| `DELETE` | `/api/v1/decorations/pet/{petId}/type/{type}` | 장식 제거 | ✅ | - |

---

## 응답 구조 및 DTO

모든 응답의 필드명은 **SNAKE_CASE** 형식입니다.

### PetResponse

기본 반려동물 정보

```json
{
  "id": 1,
  "user_id": 10,
  "owner_nickname": "pet_lover_123",
  "name": "뽀삐",
  "species": "개",
  "breed": "골든 리트리버",
  "age": 3,
  "gender": "수",
  "weight": 28.5,
  "bio": "행복한 우리 뽀삐입니다!",
  "profile_image_url": "https://cdn.example.com/pets/1/profile.jpg",
  "created_at": "2024-01-15T10:30:00Z"
}
```

| 필드 | 타입 | 설명 | 필수 |
|------|------|------|------|
| `id` | Long | 반려동물 고유 ID | ✅ |
| `user_id` | Long | 사용자 ID | ✅ |
| `owner_nickname` | String | 소유자 닉네임 | ✅ |
| `name` | String | 반려동물 이름 | ✅ |
| `species` | String | 종류 (개, 고양이 등) | ✅ |
| `breed` | String | 품종 | ❌ |
| `age` | Integer | 나이 (년) | ❌ |
| `gender` | String | 성별 (수/암) | ❌ |
| `weight` | Double | 체중 (kg) | ❌ |
| `bio` | String | 소개 | ❌ |
| `profile_image_url` | String | 프로필 이미지 URL | ❌ |
| `created_at` | LocalDateTime | 등록 시간 | ✅ |

### PetDashboardResponse

대시보드용 반려동물 정보 (PetResponse 확장)

```json
{
  "id": 1,
  "user_id": 10,
  "owner_nickname": "pet_lover_123",
  "name": "뽀삐",
  "species": "개",
  "breed": "골든 리트리버",
  "age": 3,
  "gender": "수",
  "weight": 28.5,
  "bio": "행복한 우리 뽀삐입니다!",
  "profile_image_url": "https://cdn.example.com/pets/1/profile.jpg",
  "created_at": "2024-01-15T10:30:00Z",
  "like_count": 156,
  "comment_count": 23,
  "gallery_count": 12,
  "is_liked": true,
  "decorations": [
    {
      "id": 5,
      "pet_id": 1,
      "decoration_id": 101,
      "decoration_name": "별 테두리",
      "decoration_type": "BORDER",
      "decoration_tier": "BASIC",
      "image_url": "https://cdn.example.com/decorations/border_star.png",
      "equipped_at": "2024-02-20T14:15:00Z"
    }
  ]
}
```

**추가 필드:**

| 필드 | 타입 | 설명 |
|------|------|------|
| `like_count` | Long | 좋아요 수 |
| `comment_count` | Long | 댓글 수 |
| `gallery_count` | Long | 갤러리 사진 수 |
| `is_liked` | Boolean | 현재 사용자의 좋아요 여부 |
| `decorations` | Array | 적용된 장식 배열 |

### GalleryResponse

사진 정보

```json
{
  "id": 1,
  "pet_id": 1,
  "pet_name": "뽀삐",
  "image_url": "https://cdn.example.com/pets/1/photo_1.jpg",
  "caption": "산책 중인 뽀삐",
  "created_at": "2024-02-20T15:45:00Z"
}
```

| 필드 | 타입 | 설명 | 필수 |
|------|------|------|------|
| `id` | Long | 사진 고유 ID | ✅ |
| `pet_id` | Long | 반려동물 ID | ✅ |
| `pet_name` | String | 반려동물 이름 | ✅ |
| `image_url` | String | 이미지 URL | ✅ |
| `caption` | String | 설명 | ❌ |
| `created_at` | LocalDateTime | 등록 시간 | ✅ |

### CommentResponse

댓글 정보

```json
{
  "id": 1,
  "pet_id": 1,
  "user_id": 20,
  "user_nickname": "pet_fan_456",
  "user_profile_image_url": "https://cdn.example.com/users/20/profile.jpg",
  "content": "정말 귀여운 뽀삐네요!",
  "created_at": "2024-02-20T16:30:00Z"
}
```

| 필드 | 타입 | 설명 | 필수 |
|------|------|------|------|
| `id` | Long | 댓글 고유 ID | ✅ |
| `pet_id` | Long | 반려동물 ID | ✅ |
| `user_id` | Long | 댓글 작성자 ID | ✅ |
| `user_nickname` | String | 작성자 닉네임 | ✅ |
| `user_profile_image_url` | String | 작성자 프로필 이미지 | ❌ |
| `content` | String | 댓글 내용 | ✅ |
| `created_at` | LocalDateTime | 작성 시간 | ✅ |

### LikeResponse

좋아요 정보

```json
{
  "pet_id": 1,
  "like_count": 156,
  "is_liked": true
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `pet_id` | Long | 반려동물 ID |
| `like_count` | Long | 총 좋아요 수 |
| `is_liked` | Boolean | 현재 사용자의 좋아요 여부 |

### DecorationResponse

장식 정보

```json
{
  "id": 1,
  "name": "별 테두리",
  "type": "BORDER",
  "tier": "BASIC",
  "description": "귀여운 별 모양의 테두리 장식",
  "image_url": "https://cdn.example.com/decorations/border_star.png",
  "is_default": false,
  "created_at": "2024-01-01T00:00:00Z"
}
```

| 필드 | 타입 | 설명 | 필수 |
|------|------|------|------|
| `id` | Long | 장식 고유 ID | ✅ |
| `name` | String | 장식 이름 | ✅ |
| `type` | String | 유형 (BORDER / BADGE) | ✅ |
| `tier` | String | 티어 (BASIC / PREMIUM) | ✅ |
| `description` | String | 설명 | ❌ |
| `image_url` | String | 이미지 URL | ✅ |
| `is_default` | Boolean | 기본 장식 여부 | ✅ |
| `created_at` | LocalDateTime | 생성 시간 | ✅ |

### PetDecorationResponse

반려동물에 적용된 장식

```json
{
  "id": 5,
  "pet_id": 1,
  "decoration_id": 101,
  "decoration_name": "별 테두리",
  "decoration_type": "BORDER",
  "decoration_tier": "BASIC",
  "image_url": "https://cdn.example.com/decorations/border_star.png",
  "equipped_at": "2024-02-20T14:15:00Z"
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | Long | 적용 기록 ID |
| `pet_id` | Long | 반려동물 ID |
| `decoration_id` | Long | 장식 ID |
| `decoration_name` | String | 장식 이름 |
| `decoration_type` | String | 장식 유형 |
| `decoration_tier` | String | 장식 티어 |
| `image_url` | String | 이미지 URL |
| `equipped_at` | LocalDateTime | 적용 시간 |

### TokenResponse

인증 토큰 응답

```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer"
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `access_token` | String | 접근 토큰 (15분 유효) |
| `refresh_token` | String | 갱신 토큰 |
| `token_type` | String | 토큰 타입 (항상 "Bearer") |

### UserResponse

사용자 정보

```json
{
  "id": 10,
  "email": "user@example.com",
  "nickname": "pet_lover_123",
  "profile_image_url": "https://cdn.example.com/users/10/profile.jpg",
  "bio": "반려동물 사랑하는 사람",
  "created_at": "2024-01-10T09:00:00Z"
}
```

### ErrorResponse

에러 응답

```json
{
  "status": 400,
  "message": "이메일 형식이 유효하지 않습니다."
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `status` | Integer | HTTP 상태 코드 |
| `message` | String | 에러 메시지 |

---

## 자유 플랜 제한 사항

Bridgy는 자유 플랜과 프리미엄 플랜을 구분합니다. 모든 사용자는 기본적으로 자유 플랜입니다.

### 제한 사항 (FreeTierLimits.kt)

| 제한사항 | 한도 | 설명 |
|---------|------|------|
| `MAX_PETS_PER_USER` | **3마리** | 사용자당 등록 가능한 반려동물 수 |
| `MAX_PHOTOS_PER_PET` | **20장** | 반려동물당 업로드 가능한 사진 수 |
| `MAX_COMMENTS_PER_USER_PER_DAY` | **30개** | 사용자가 하루에 작성할 수 있는 댓글 수 |
| `MAX_COMMENT_LENGTH` | **300자** | 댓글의 최대 길이 |
| `MAX_PAGE_SIZE` | **20** | 페이지네이션 최대 크기 (피드, 검색) |
| **장식 (Decoration)** | **BASIC만** | 프리미엄 장식은 403 Forbidden 반환 |

### 제한 초과 시 발생하는 오류

- **409 CONFLICT**: 최대 반려동물 수 초과 (3마리 이상 등록 시도)
- **409 CONFLICT**: 반려동물당 최대 사진 수 초과 (20장 이상 업로드 시도)
- **429 TOO_MANY_REQUESTS**: 일일 댓글 한도 초과 (30개 이상 작성 시도)
- **400 BAD_REQUEST**: 댓글 길이 초과 (300자 초과 시도)
- **403 FORBIDDEN**: 프리미엄 장식 사용 시도

---

## 에러 처리

### HTTP 상태 코드

| 상태코드 | 의미 | 예시 |
|---------|------|------|
| **200 OK** | 성공 | 조회, 수정 성공 |
| **201 Created** | 리소스 생성됨 | 반려동물, 댓글, 사진 생성 |
| **204 No Content** | 성공 (응답 본문 없음) | 삭제, 로그아웃 |
| **400 Bad Request** | 잘못된 요청 | 유효하지 않은 입력, 제한 초과 |
| **401 Unauthorized** | 인증 실패 | 토큰 없음, 토큰 만료 |
| **403 Forbidden** | 접근 거부 | 프리미엄 기능 사용 시도 |
| **404 Not Found** | 리소스 없음 | 존재하지 않는 반려동물 |
| **409 Conflict** | 충돌 | 중복 이메일, 최대 반려동물 수 초과 |
| **429 Too Many Requests** | 속도 제한 | 일일 댓글 한도 초과 |
| **500 Internal Server Error** | 서버 오류 | 서버 에러 |

### 에러 응답 예시

#### 인증 실패 (401)

```json
{
  "status": 401,
  "message": "유효하지 않은 또는 만료된 토큰입니다."
}
```

#### 검증 오류 (400)

```json
{
  "status": 400,
  "message": "반려동물당 최대 20장의 사진만 업로드할 수 있습니다. (현재: 20장)"
}
```

#### 리소스 없음 (404)

```json
{
  "status": 404,
  "message": "해당 반려동물을 찾을 수 없습니다. (ID: 999)"
}
```

#### 제한 초과 (429)

```json
{
  "status": 429,
  "message": "일일 댓글 한도(30개)를 초과했습니다."
}
```

#### 프리미엄 기능 접근 시도 (403)

```json
{
  "status": 403,
  "message": "이 기능은 프리미엄 회원만 사용할 수 있습니다."
}
```

### 프론트엔드 에러 처리 팁

```dart
// Flutter 예시
try {
  final response = await http.post(
    Uri.parse('http://localhost:8080/api/v1/pets'),
    headers: {'Authorization': 'Bearer $accessToken'},
    body: jsonEncode(petData),
  );

  if (response.statusCode == 201) {
    // 성공
    final pet = PetResponse.fromJson(jsonDecode(response.body));
  } else if (response.statusCode == 401) {
    // 토큰 만료 → 갱신 후 재시도
    await refreshToken();
  } else if (response.statusCode == 409) {
    // 최대 반려동물 수 초과
    showError('최대 3마리까지만 등록할 수 있습니다.');
  } else if (response.statusCode == 429) {
    // 일일 한도 초과
    showError('일일 댓글 한도를 초과했습니다.');
  } else {
    // 기타 에러
    final error = ErrorResponse.fromJson(jsonDecode(response.body));
    showError(error.message);
  }
} catch (e) {
  // 네트워크 에러
  showError('네트워크 오류가 발생했습니다.');
}
```

---

## 페이지네이션

### Page<> 응답 형식

검색, 피드 조회 등에서 페이지네이션된 응답을 받습니다.

```json
{
  "content": [
    {
      "id": 1,
      "name": "뽀삐",
      "species": "개",
      "...": "..."
    },
    {
      "id": 2,
      "name": "나비",
      "species": "고양이",
      "...": "..."
    }
  ],
  "number": 0,
  "size": 10,
  "total_elements": 45,
  "total_pages": 5,
  "is_first": true,
  "is_last": false,
  "has_next": true,
  "has_previous": false
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `content` | Array | 현재 페이지의 데이터 배열 |
| `number` | Integer | 현재 페이지 번호 (0부터 시작) |
| `size` | Integer | 한 페이지당 데이터 수 |
| `total_elements` | Long | 전체 데이터 수 |
| `total_pages` | Integer | 전체 페이지 수 |
| `is_first` | Boolean | 첫 페이지 여부 |
| `is_last` | Boolean | 마지막 페이지 여부 |
| `has_next` | Boolean | 다음 페이지 존재 여부 |
| `has_previous` | Boolean | 이전 페이지 존재 여부 |

### 페이지네이션 매개변수

페이지네이션을 사용하는 엔드포인트에서 다음 매개변수를 사용합니다.

| 매개변수 | 타입 | 기본값 | 최대값 | 설명 |
|---------|------|-------|-------|------|
| `page` | Integer | 0 | - | 페이지 번호 (0부터 시작) |
| `size` | Integer | 10 | 20 | 한 페이지당 데이터 수 |

### 페이지네이션 사용 예시

#### 2번째 페이지, 15개씩 조회

```http
GET /api/v0/pets/search?name=뽀&page=1&size=15
```

#### 3번째 페이지, 기본 크기(10개)로 조회

```http
GET /api/v0/pets/feed/popular?userId=10&page=2
```

#### Flutter 클라이언트 예시

```dart
Future<List<PetResponse>> fetchNextPage(int pageNumber) async {
  final response = await http.get(
    Uri.parse('http://localhost:8080/api/v0/pets/search?name=뽀&page=$pageNumber&size=10'),
  );

  if (response.statusCode == 200) {
    final data = jsonDecode(response.body);
    final pageData = PageResponse<PetResponse>.fromJson(data);
    
    // 다음 페이지가 있는지 확인
    if (!pageData.isLast) {
      // 다음 페이지 로드
      await fetchNextPage(pageNumber + 1);
    }
    
    return pageData.content;
  } else {
    throw Exception('Failed to load pets');
  }
}
```

---

## 사용 예시

### 예시 1: 회원가입 및 로그인

#### 1. 회원가입

```http
POST /api/v0/auth/signup
Content-Type: application/json

{
  "email": "newuser@example.com",
  "password": "secure_password_123",
  "nickname": "pet_lover_123"
}
```

**응답 (201 Created)**
```json
{
  "id": 1,
  "email": "newuser@example.com",
  "nickname": "pet_lover_123",
  "profile_image_url": null,
  "bio": null,
  "created_at": "2024-02-21T10:00:00Z"
}
```

#### 2. 로그인

```http
POST /api/v0/auth/login
Content-Type: application/json

{
  "email": "newuser@example.com",
  "password": "secure_password_123"
}
```

**응답 (200 OK)**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwiaWF0IjoxNjA1NDA1MjAwfQ.abcdefg...",
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwiaWF0IjoxNjA1NDA1MjAwfQ.hijklmn...",
  "token_type": "Bearer"
}
```

#### 3. 인증된 요청 (사용자 정보 조회)

```http
GET /api/v1/users/1
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwiaWF0IjoxNjA1NDA1MjAwfQ.abcdefg...
```

**응답 (200 OK)**
```json
{
  "id": 1,
  "email": "newuser@example.com",
  "nickname": "pet_lover_123",
  "profile_image_url": null,
  "bio": null,
  "created_at": "2024-02-21T10:00:00Z"
}
```

---

### 예시 2: 반려동물 등록 및 조회

#### 1. 반려동물 등록

```http
POST /api/v1/pets
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "name": "뽀삐",
  "species": "개",
  "breed": "골든 리트리버",
  "age": 3,
  "gender": "수",
  "weight": 28.5,
  "bio": "행복한 우리 뽀삐입니다!"
}
```

**응답 (201 Created)**
```json
{
  "id": 101,
  "user_id": 1,
  "owner_nickname": "pet_lover_123",
  "name": "뽀삐",
  "species": "개",
  "breed": "골든 리트리버",
  "age": 3,
  "gender": "수",
  "weight": 28.5,
  "bio": "행복한 우리 뽀삐입니다!",
  "profile_image_url": null,
  "created_at": "2024-02-21T11:00:00Z"
}
```

#### 2. 반려동물 목록 조회

```http
GET /api/v0/pets/user/1
```

**응답 (200 OK)**
```json
{
  "content": [
    {
      "id": 101,
      "user_id": 1,
      "owner_nickname": "pet_lover_123",
      "name": "뽀삐",
      "species": "개",
      "breed": "골든 리트리버",
      "age": 3,
      "gender": "수",
      "weight": 28.5,
      "bio": "행복한 우리 뽀삐입니다!",
      "profile_image_url": null,
      "created_at": "2024-02-21T11:00:00Z"
    }
  ],
  "number": 0,
  "size": 10,
  "total_elements": 1,
  "total_pages": 1,
  "is_first": true,
  "is_last": true,
  "has_next": false,
  "has_previous": false
}
```

#### 3. 반려동물 상세 조회

```http
GET /api/v0/pets/101/detail?userId=1
```

**응답 (200 OK)**
```json
{
  "id": 101,
  "user_id": 1,
  "owner_nickname": "pet_lover_123",
  "name": "뽀삐",
  "species": "개",
  "breed": "골든 리트리버",
  "age": 3,
  "gender": "수",
  "weight": 28.5,
  "bio": "행복한 우리 뽀삐입니다!",
  "profile_image_url": null,
  "created_at": "2024-02-21T11:00:00Z",
  "like_count": 42,
  "comment_count": 8,
  "gallery_count": 5,
  "is_liked": true,
  "decorations": [
    {
      "id": 1,
      "pet_id": 101,
      "decoration_id": 50,
      "decoration_name": "별 테두리",
      "decoration_type": "BORDER",
      "decoration_tier": "BASIC",
      "image_url": "https://cdn.example.com/decorations/border_star.png",
      "equipped_at": "2024-02-21T11:30:00Z"
    }
  ]
}
```

---

### 예시 3: 사진 업로드 및 조회

#### 1. 사진 업로드

```http
POST /api/v1/gallery
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "pet_id": 101,
  "image_url": "https://example.com/images/puppy.jpg",
  "caption": "산책 중인 뽀삐"
}
```

**응답 (201 Created)**
```json
{
  "id": 1001,
  "pet_id": 101,
  "pet_name": "뽀삐",
  "image_url": "https://example.com/images/puppy.jpg",
  "caption": "산책 중인 뽀삐",
  "created_at": "2024-02-21T12:00:00Z"
}
```

#### 2. 반려동물 사진 조회

```http
GET /api/v0/gallery/pet/101
```

**응답 (200 OK)**
```json
[
  {
    "id": 1001,
    "pet_id": 101,
    "pet_name": "뽀삐",
    "image_url": "https://example.com/images/puppy.jpg",
    "caption": "산책 중인 뽀삐",
    "created_at": "2024-02-21T12:00:00Z"
  },
  {
    "id": 1002,
    "pet_id": 101,
    "pet_name": "뽀삐",
    "image_url": "https://example.com/images/puppy2.jpg",
    "caption": "잠자는 뽀삐",
    "created_at": "2024-02-21T13:00:00Z"
  }
]
```

---

### 예시 4: 댓글 작성 및 조회

#### 1. 댓글 작성

```http
POST /api/v1/comments
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "pet_id": 101,
  "content": "정말 귀여운 뽀삐네요! 저도 골든 리트리버를 키워요."
}
```

**응답 (201 Created)**
```json
{
  "id": 5001,
  "pet_id": 101,
  "user_id": 2,
  "user_nickname": "pet_fan_456",
  "user_profile_image_url": "https://cdn.example.com/users/2/profile.jpg",
  "content": "정말 귀여운 뽀삐네요! 저도 골든 리트리버를 키워요.",
  "created_at": "2024-02-21T14:00:00Z"
}
```

#### 2. 댓글 조회

```http
GET /api/v0/comments/pet/101
```

**응답 (200 OK)**
```json
[
  {
    "id": 5001,
    "pet_id": 101,
    "user_id": 2,
    "user_nickname": "pet_fan_456",
    "user_profile_image_url": "https://cdn.example.com/users/2/profile.jpg",
    "content": "정말 귀여운 뽀삐네요! 저도 골든 리트리버를 키워요.",
    "created_at": "2024-02-21T14:00:00Z"
  },
  {
    "id": 5002,
    "pet_id": 101,
    "user_id": 3,
    "user_nickname": "dog_lover_789",
    "user_profile_image_url": null,
    "content": "너무 예쁜 반려견이네요!",
    "created_at": "2024-02-21T14:30:00Z"
  }
]
```

#### 3. 댓글 수 조회

```http
GET /api/v0/comments/pet/101/count
```

**응답 (200 OK)**
```
2
```

---

### 예시 5: 좋아요 (토글)

#### 1. 좋아요 추가/제거

```http
POST /api/v1/likes
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "pet_id": 101
}
```

**응답 (201 Created)** (좋아요 추가됨)
```json
{
  "pet_id": 101,
  "like_count": 43,
  "is_liked": true
}
```

**응답 (201 Created)** (좋아요 제거됨, 다시 요청했을 때)
```json
{
  "pet_id": 101,
  "like_count": 42,
  "is_liked": false
}
```

#### 2. 좋아요 상태 조회

```http
GET /api/v0/likes/pet/101/user/1
```

**응답 (200 OK)**
```json
{
  "pet_id": 101,
  "like_count": 42,
  "is_liked": true
}
```

---

### 예시 6: 피드 조회 (인기순/최신순)

#### 1. 인기순 피드 (좋아요 많은 순)

```http
GET /api/v0/pets/feed/popular?userId=1&page=0&size=10
```

**응답 (200 OK)**
```json
{
  "content": [
    {
      "id": 101,
      "user_id": 5,
      "owner_nickname": "super_pet_fan",
      "name": "뽀삐",
      "species": "개",
      "breed": "골든 리트리버",
      "age": 3,
      "gender": "수",
      "weight": 28.5,
      "bio": "행복한 우리 뽀삐입니다!",
      "profile_image_url": "https://cdn.example.com/pets/101/profile.jpg",
      "created_at": "2024-02-21T11:00:00Z",
      "like_count": 156,
      "comment_count": 23,
      "gallery_count": 12,
      "is_liked": true,
      "decorations": [
        {
          "id": 1,
          "pet_id": 101,
          "decoration_id": 50,
          "decoration_name": "별 테두리",
          "decoration_type": "BORDER",
          "decoration_tier": "BASIC",
          "image_url": "https://cdn.example.com/decorations/border_star.png",
          "equipped_at": "2024-02-21T11:30:00Z"
        }
      ]
    },
    {
      "id": 102,
      "user_id": 6,
      "owner_nickname": "cat_lover_123",
      "name": "나비",
      "species": "고양이",
      "breed": "페르시안",
      "age": 2,
      "gender": "암",
      "weight": 4.2,
      "bio": "귀여운 고양이 나비입니다.",
      "profile_image_url": "https://cdn.example.com/pets/102/profile.jpg",
      "created_at": "2024-02-20T15:00:00Z",
      "like_count": 98,
      "comment_count": 15,
      "gallery_count": 8,
      "is_liked": false,
      "decorations": []
    }
  ],
  "number": 0,
  "size": 10,
  "total_elements": 45,
  "total_pages": 5,
  "is_first": true,
  "is_last": false,
  "has_next": true,
  "has_previous": false
}
```

#### 2. 최신순 피드 (최근 등록순)

```http
GET /api/v0/pets/feed/recent?userId=1&page=0&size=10
```

응답 형식은 위와 동일하지만, 정렬 순서가 최근 등록순입니다.

---

### 예시 7: 반려동물 검색

```http
GET /api/v0/pets/search?name=뽀&page=0&size=10
```

**응답 (200 OK)**
```json
{
  "content": [
    {
      "id": 101,
      "user_id": 5,
      "owner_nickname": "super_pet_fan",
      "name": "뽀삐",
      "species": "개",
      "breed": "골든 리트리버",
      "age": 3,
      "gender": "수",
      "weight": 28.5,
      "bio": "행복한 우리 뽀삐입니다!",
      "profile_image_url": "https://cdn.example.com/pets/101/profile.jpg",
      "created_at": "2024-02-21T11:00:00Z"
    }
  ],
  "number": 0,
  "size": 10,
  "total_elements": 1,
  "total_pages": 1,
  "is_first": true,
  "is_last": true,
  "has_next": false,
  "has_previous": false
}
```

---

### 예시 8: 장식 적용

#### 1. 사용 가능한 기본 장식 조회

```http
GET /api/v0/decorations/basic
```

**응답 (200 OK)**
```json
[
  {
    "id": 50,
    "name": "별 테두리",
    "type": "BORDER",
    "tier": "BASIC",
    "description": "귀여운 별 모양의 테두리 장식",
    "image_url": "https://cdn.example.com/decorations/border_star.png",
    "is_default": false,
    "created_at": "2024-01-01T00:00:00Z"
  },
  {
    "id": 51,
    "name": "하트 배지",
    "type": "BADGE",
    "tier": "BASIC",
    "description": "사랑스러운 하트 배지",
    "image_url": "https://cdn.example.com/decorations/badge_heart.png",
    "is_default": false,
    "created_at": "2024-01-01T00:00:00Z"
  }
]
```

#### 2. 반려동물에 장식 적용

```http
POST /api/v1/decorations/equip
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "pet_id": 101,
  "decoration_id": 50
}
```

**응답 (201 Created)**
```json
{
  "id": 1,
  "pet_id": 101,
  "decoration_id": 50,
  "decoration_name": "별 테두리",
  "decoration_type": "BORDER",
  "decoration_tier": "BASIC",
  "image_url": "https://cdn.example.com/decorations/border_star.png",
  "equipped_at": "2024-02-21T15:00:00Z"
}
```

#### 3. 반려동물 장식 조회

```http
GET /api/v0/decorations/pet/101
```

**응답 (200 OK)**
```json
[
  {
    "id": 1,
    "pet_id": 101,
    "decoration_id": 50,
    "decoration_name": "별 테두리",
    "decoration_type": "BORDER",
    "decoration_tier": "BASIC",
    "image_url": "https://cdn.example.com/decorations/border_star.png",
    "equipped_at": "2024-02-21T15:00:00Z"
  }
]
```

#### 4. 장식 제거

```http
DELETE /api/v1/decorations/pet/101/type/BORDER
Authorization: Bearer <access_token>
```

**응답 (204 No Content)**

---

## 프론트엔드 개발자를 위한 가이드

### 🛠️ 개발 환경 설정

#### 1. 로컬 서버 실행

```bash
cd /path/to/bridgy
docker-compose up -d
./gradlew bootRun --args='--spring.profiles.active=local'
```

서버가 `http://localhost:8080`에서 실행됩니다.

#### 2. API 문서 확인

Swagger UI를 열어서 모든 엔드포인트를 확인합니다.

```
http://localhost:8080/swagger-ui/index.html
```

여기서 직접 API를 테스트할 수 있습니다.

### 🔄 토큰 관리 전략

#### Access Token과 Refresh Token 차이

| 토큰 | 유효시간 | 용도 | 저장 위치 |
|------|---------|------|---------|
| **Access Token** | 15분 | API 요청 시 `Authorization` 헤더에 포함 | 메모리 (앱 종료 시 삭제) |
| **Refresh Token** | 길음 (예: 30일) | Access Token 만료 시 새로운 Access Token 발급 | 안전한 저장소 (보안 스토리지) |

#### 구현 예시 (Flutter)

```dart
class AuthService {
  String? _accessToken;
  String? _refreshToken;

  // 로그인
  Future<void> login(String email, String password) async {
    final response = await http.post(
      Uri.parse('http://localhost:8080/api/v0/auth/login'),
      body: jsonEncode({
        'email': email,
        'password': password,
      }),
      headers: {'Content-Type': 'application/json'},
    );

    if (response.statusCode == 200) {
      final data = jsonDecode(response.body);
      _accessToken = data['access_token'];
      _refreshToken = data['refresh_token'];
      
      // Refresh Token은 보안 스토리지에 저장
      await _secureStorage.write(
        key: 'refresh_token',
        value: _refreshToken,
      );
    }
  }

  // 토큰 갱신
  Future<void> refreshAccessToken() async {
    final refreshToken = await _secureStorage.read(key: 'refresh_token');
    
    final response = await http.post(
      Uri.parse('http://localhost:8080/api/v0/auth/refresh'),
      body: jsonEncode({'refresh_token': refreshToken}),
      headers: {'Content-Type': 'application/json'},
    );

    if (response.statusCode == 200) {
      final data = jsonDecode(response.body);
      _accessToken = data['access_token'];
      _refreshToken = data['refresh_token'];
      
      // 새 Refresh Token도 저장
      await _secureStorage.write(
        key: 'refresh_token',
        value: _refreshToken,
      );
    }
  }

  // 인증된 요청
  Future<http.Response> authenticatedRequest(
    String method,
    String path, {
    Map<String, dynamic>? body,
  }) async {
    var response = await _makeRequest(method, path, body: body);

    // Access Token 만료 시 갱신 후 재시도
    if (response.statusCode == 401) {
      await refreshAccessToken();
      response = await _makeRequest(method, path, body: body);
    }

    return response;
  }

  Future<http.Response> _makeRequest(
    String method,
    String path, {
    Map<String, dynamic>? body,
  }) async {
    final headers = {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer $_accessToken',
    };

    final uri = Uri.parse('http://localhost:8080$path');

    if (method == 'GET') {
      return http.get(uri, headers: headers);
    } else if (method == 'POST') {
      return http.post(uri, headers: headers, body: jsonEncode(body));
    } else if (method == 'PUT') {
      return http.put(uri, headers: headers, body: jsonEncode(body));
    } else if (method == 'DELETE') {
      return http.delete(uri, headers: headers);
    }

    throw Exception('Unsupported HTTP method: $method');
  }

  // 로그아웃
  Future<void> logout() async {
    await authenticatedRequest('POST', '/api/v1/auth/logout');
    _accessToken = null;
    _refreshToken = null;
    await _secureStorage.delete(key: 'refresh_token');
  }
}
```

### 📱 UI 구현 시 주의사항

#### 1. SNAKE_CASE 필드명 처리

모든 JSON 응답의 필드는 SNAKE_CASE입니다. Dart 모델을 정의할 때 `@JsonKey` 어노테이션을 사용하세요.

```dart
import 'package:json_annotation/json_annotation.dart';

part 'pet_response.g.dart';

@JsonSerializable()
class PetResponse {
  final int id;
  
  @JsonKey(name: 'user_id')
  final int userId;
  
  @JsonKey(name: 'owner_nickname')
  final String ownerNickname;
  
  final String name;
  final String species;
  final String? breed;
  final int? age;
  final String? gender;
  final double? weight;
  final String? bio;
  
  @JsonKey(name: 'profile_image_url')
  final String? profileImageUrl;
  
  @JsonKey(name: 'created_at')
  final DateTime createdAt;

  PetResponse({
    required this.id,
    required this.userId,
    required this.ownerNickname,
    required this.name,
    required this.species,
    this.breed,
    this.age,
    this.gender,
    this.weight,
    this.bio,
    this.profileImageUrl,
    required this.createdAt,
  });

  factory PetResponse.fromJson(Map<String, dynamic> json) =>
      _$PetResponseFromJson(json);

  Map<String, dynamic> toJson() => _$PetResponseToJson(this);
}
```

#### 2. 제한 사항 처리

자유 플랜 제한을 UI에 반영합니다.

```dart
class PetManagementPage extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return FutureBuilder<List<PetResponse>>(
      future: _fetchUserPets(),
      builder: (context, snapshot) {
        if (snapshot.hasData) {
          final pets = snapshot.data!;
          final canAddMore = pets.length < 3; // MAX_PETS_PER_USER = 3

          return Column(
            children: [
              ListView.builder(
                itemCount: pets.length,
                itemBuilder: (context, index) => PetCard(pet: pets[index]),
              ),
              if (canAddMore)
                ElevatedButton(
                  onPressed: () => _showAddPetDialog(context),
                  child: const Text('반려동물 추가'),
                )
              else
                Container(
                  padding: const EdgeInsets.all(16),
                  color: Colors.amber[100],
                  child: const Text(
                    '무료 플랜에서는 최대 3마리까지만 등록할 수 있습니다.',
                    textAlign: TextAlign.center,
                  ),
                ),
            ],
          );
        } else if (snapshot.hasError) {
          return Text('오류: ${snapshot.error}');
        } else {
          return const Center(child: CircularProgressIndicator());
        }
      },
    );
  }
}
```

#### 3. 에러 처리 UI

HTTP 상태 코드에 따라 사용자 친화적인 메시지를 표시합니다.

```dart
void _handleApiError(dynamic error, int? statusCode) {
  String message;

  switch (statusCode) {
    case 400:
      message = '요청이 유효하지 않습니다. 입력값을 확인해주세요.';
      break;
    case 401:
      message = '인증이 필요합니다. 다시 로그인해주세요.';
      // 재로그인 화면으로 이동
      break;
    case 403:
      message = '이 기능은 프리미엄 회원만 사용할 수 있습니다.';
      break;
    case 404:
      message = '요청한 리소스를 찾을 수 없습니다.';
      break;
    case 409:
      message = '요청이 충돌합니다. 반려동물 수 제한을 확인해주세요.';
      break;
    case 429:
      message = '요청이 너무 많습니다. 잠시 후 다시 시도해주세요.';
      break;
    default:
      message = '오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
  }

  ScaffoldMessenger.of(context).showSnackBar(
    SnackBar(content: Text(message)),
  );
}
```

### 📊 페이지네이션 구현

```dart
class PetFeedPage extends StatefulWidget {
  @override
  State<PetFeedPage> createState() => _PetFeedPageState();
}

class _PetFeedPageState extends State<PetFeedPage> {
  late ScrollController _scrollController;
  List<PetDashboardResponse> _pets = [];
  int _currentPage = 0;
  bool _hasMorePages = true;
  bool _isLoading = false;

  @override
  void initState() {
    super.initState();
    _scrollController = ScrollController();
    _scrollController.addListener(_onScroll);
    _loadPopularFeed();
  }

  void _onScroll() {
    // 하단에 도달하면 다음 페이지 로드
    if (_scrollController.position.pixels ==
        _scrollController.position.maxScrollExtent) {
      if (_hasMorePages && !_isLoading) {
        _loadPopularFeed();
      }
    }
  }

  Future<void> _loadPopularFeed() async {
    if (_isLoading) return;
    setState(() => _isLoading = true);

    try {
      final response = await http.get(
        Uri.parse(
          'http://localhost:8080/api/v0/pets/feed/popular'
          '?userId=1&page=$_currentPage&size=10',
        ),
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        final pageData = PageResponse<PetDashboardResponse>.fromJson(data);

        setState(() {
          _pets.addAll(pageData.content);
          _currentPage++;
          _hasMorePages = pageData.hasNext;
        });
      }
    } catch (e) {
      _handleApiError(e, null);
    } finally {
      setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return ListView.builder(
      controller: _scrollController,
      itemCount: _pets.length + (_isLoading ? 1 : 0),
      itemBuilder: (context, index) {
        if (index == _pets.length) {
          return const Center(child: CircularProgressIndicator());
        }
        return PetCard(pet: _pets[index]);
      },
    );
  }

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }
}
```

### 🖼️ 이미지 업로드 구현

사진 업로드는 일반적으로 다음 단계로 진행됩니다:

1. 이미지를 별도 스토리지(예: Firebase Storage, AWS S3)에 업로드
2. 반환된 이미지 URL을 `/api/v1/gallery` 엔드포인트에 전달

```dart
Future<void> uploadPhoto(int petId, File imageFile, String caption) async {
  try {
    // 1. 이미지를 클라우드 스토리지에 업로드
    final storageRef = FirebaseStorage.instance.ref();
    final uploadTask = storageRef
        .child('pets/$petId/${DateTime.now().millisecondsSinceEpoch}.jpg')
        .putFile(imageFile);

    final taskSnapshot = await uploadTask;
    final imageUrl = await taskSnapshot.ref.getDownloadURL();

    // 2. 이미지 URL과 함께 API에 전송
    final response = await http.post(
      Uri.parse('http://localhost:8080/api/v1/gallery'),
      headers: {
        'Authorization': 'Bearer $accessToken',
        'Content-Type': 'application/json',
      },
      body: jsonEncode({
        'pet_id': petId,
        'image_url': imageUrl,
        'caption': caption,
      }),
    );

    if (response.statusCode == 201) {
      print('사진 업로드 성공');
    } else if (response.statusCode == 409) {
      print('반려동물당 최대 20장까지만 업로드할 수 있습니다.');
    }
  } catch (e) {
    print('오류: $e');
  }
}
```

---

## 프로젝트 구조

```
src/main/kotlin/org/grr/bridgy/
├── BridgyApplication.kt                # 메인 애플리케이션
│
├── common/                              # 공통 기능
│   ├── config/
│   │   ├── FreeTierLimits.kt           # 자유 플랜 제한값
│   │   ├── RedisConfig.kt              # Redis 캐시 설정
│   │   ├── SecurityConfig.kt           # Spring Security & JWT 설정
│   │   ├── SwaggerConfig.kt            # Swagger/OpenAPI 설정
│   │   ├── CorsConfig.kt               # CORS 정책
│   │   └── GlobalExceptionHandler.kt   # 전역 예외 처리
│   ├── exception/
│   │   ├── CustomException.kt          # 커스텀 예외 클래스
│   │   └── ErrorResponse.kt            # 에러 응답 DTO
│   ├── jwt/
│   │   ├── JwtFilter.kt                # JWT 필터
│   │   └── JwtProvider.kt              # JWT 토큰 생성/검증
│   └── BaseTime.kt                     # 공통 시간 필드 (createdAt, updatedAt)
│
└── domain/                              # 도메인별 기능
    ├── auth/                            # 인증
    │   ├── controller/
    │   │   └── AuthController.kt
    │   ├── dto/
    │   │   ├── TokenResponse.kt
    │   │   ├── LoginRequest.kt
    │   │   └── SignupRequest.kt
    │   ├── entity/
    │   │   └── User.kt
    │   ├── repository/
    │   │   └── UserRepository.kt
    │   └── service/
    │       └── AuthService.kt
    │
    ├── user/                            # 사용자 프로필
    │   ├── controller/
    │   │   └── UserController.kt
    │   ├── dto/
    │   │   ├── UserResponse.kt
    │   │   └── UserUpdateRequest.kt
    │   ├── entity/
    │   │   └── User.kt
    │   ├── repository/
    │   │   └── UserRepository.kt
    │   └── service/
    │       └── UserService.kt
    │
    ├── pet/                             # 반려동물 프로필 & 대시보드
    │   ├── controller/
    │   │   └── PetController.kt
    │   ├── dto/
    │   │   ├── PetResponse.kt
    │   │   ├── PetDashboardResponse.kt
    │   │   ├── PetCreateRequest.kt
    │   │   └── PetUpdateRequest.kt
    │   ├── entity/
    │   │   └── Pet.kt
    │   ├── repository/
    │   │   └── PetRepository.kt
    │   └── service/
    │       └── PetService.kt
    │
    ├── gallery/                         # 사진 갤러리
    │   ├── controller/
    │   │   └── GalleryController.kt
    │   ├── dto/
    │   │   ├── GalleryResponse.kt
    │   │   └── GalleryUploadRequest.kt
    │   ├── entity/
    │   │   └── Gallery.kt
    │   ├── repository/
    │   │   └── GalleryRepository.kt
    │   └── service/
    │       └── GalleryService.kt
    │
    ├── comment/                         # 댓글
    │   ├── controller/
    │   │   └── CommentController.kt
    │   ├── dto/
    │   │   ├── CommentResponse.kt
    │   │   ├── CommentCreateRequest.kt
    │   │   └── CommentUpdateRequest.kt
    │   ├── entity/
    │   │   └── Comment.kt
    │   ├── repository/
    │   │   └── CommentRepository.kt
    │   └── service/
    │       └── CommentService.kt
    │
    ├── like/                            # 좋아요
    │   ├── controller/
    │   │   └── LikeController.kt
    │   ├── dto/
    │   │   └── LikeResponse.kt
    │   ├── entity/
    │   │   └── Like.kt
    │   ├── repository/
    │   │   └── LikeRepository.kt
    │   └── service/
    │       └── LikeService.kt
    │
    └── decoration/                      # 장식 (프로필 커스터마이징)
        ├── controller/
        │   └── DecorationController.kt
        ├── dto/
        │   ├── DecorationResponse.kt
        │   ├── PetDecorationResponse.kt
        │   └── DecorationEquipRequest.kt
        ├── entity/
        │   ├── Decoration.kt
        │   └── PetDecoration.kt
        ├── repository/
        │   ├── DecorationRepository.kt
        │   └── PetDecorationRepository.kt
        └── service/
            └── DecorationService.kt
```

---

## 참고 자료

- **GitHub**: https://github.com/choiKYeon/bridgy
- **Swagger UI**: `http://localhost:8080/swagger-ui/index.html`
- **API Base URL (로컬)**: `http://localhost:8080`
- **PostgreSQL**: `localhost:5432`
- **Redis**: `localhost:6379`

---

## 피드백 및 문의

개발 중 문제가 발생하거나 API 변경이 필요하면 GitHub Issues를 통해 문의해주세요.
