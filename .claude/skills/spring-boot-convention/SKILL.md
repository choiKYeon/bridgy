---
name: spring-boot-convention
description: "Spring Boot + Kotlin 프로젝트 컨벤션 스킬. 엔티티 생성, API 컨트롤러 작성, JWT 인증 설정, API 버전 관리 등 프로젝트 구조 관련 작업 시 반드시 사용. 새 엔티티 추가, 컨트롤러 생성, 보안 설정, 인증/인가 구현, API 경로 설정, BaseTime 상속 등의 키워드가 나오면 이 스킬을 참고할 것."
---

# Spring Boot + Kotlin 프로젝트 컨벤션

이 스킬은 Bridgy 프로젝트에서 확립된 코드 작성 규칙을 정리한 것이다. 새로운 도메인이나 기능을 추가할 때 이 규칙을 따르면 프로젝트 전체의 일관성을 유지할 수 있다.

## 1. BaseTime 상속 규칙

모든 JPA 엔티티는 `BaseTime` 추상 클래스를 상속받는다. 엔티티에 `createdAt`/`updatedAt`을 직접 선언하지 않는다.

- 클래스명은 `BaseTime`이다 (BaseTimeEntity 아님)
- 위치: `domain/common/BaseTime.kt`
- `@MappedSuperclass` + `@EntityListeners(AuditingEntityListener::class)` 사용
- `@CreatedDate`, `@LastModifiedDate` 어노테이션으로 자동 시간 관리
- `BridgyApplication.kt`에 `@EnableJpaAuditing` 필수

**예시:**
```kotlin
@Entity
@Table(name = "users")
class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    val email: String
) : BaseTime()
```

이렇게 하는 이유: 시간 관리 코드를 한 곳에서 관리하면 실수를 줄이고, 엔티티가 깔끔해진다. Hibernate가 자동으로 시간을 채워주므로 Service 레이어에서 수동으로 `updatedAt = LocalDateTime.now()` 같은 코드를 쓸 필요가 없다.

## 2. API 버전 관리 규칙

모든 API 경로에는 반드시 버전 접두사가 붙는다.

| 버전 | 의미 | 인증 | 예시 |
|------|------|------|------|
| `v0` | 공개 API | 불필요 | `/api/v0/users/signup` |
| `v1` | 인증 필요 API | JWT Access Token 필수 | `/api/v1/pets` |
| `v2+` | 향후 버전업 시 사용 | 상황에 따라 | `/api/v2/pets` |

**경로 패턴:** `/api/{version}/{도메인}/{리소스}`

SecurityConfig에서는 `/api/v0/**`만 `permitAll()`이고, 나머지는 전부 `authenticated()`로 설정한다. 이 한 줄의 규칙으로 인증 여부가 결정되므로 새 엔드포인트를 추가할 때 SecurityConfig를 별도로 수정할 필요가 없다.

## 3. 컨트롤러 분리 규칙

하나의 도메인에 V0(공개)와 V1(인증) 엔드포인트가 모두 있으면 **컨트롤러 클래스를 분리**한다.

- 인증 불필요: `{Domain}ControllerV0` → `@RequestMapping("/api/v0/{domain}")`
- 인증 필요: `{Domain}ControllerV1` → `@RequestMapping("/api/v1/{domain}")`

**예시 - PetControllerV0.kt:**
```kotlin
@Tag(name = "Pet V0", description = "반려동물 공개 API")
@RestController
@RequestMapping("/api/v0/pets")
class PetControllerV0(private val petService: PetService) {

    @GetMapping("/{petId}")
    fun getPet(@PathVariable petId: Long): ResponseEntity<PetResponse> {
        return ResponseEntity.ok(petService.getPetById(petId))
    }
}
```

**예시 - PetControllerV1.kt:**
```kotlin
@Tag(name = "Pet V1", description = "반려동물 인증 API")
@RestController
@RequestMapping("/api/v1/pets")
class PetControllerV1(private val petService: PetService) {

    @PostMapping
    fun createPet(@RequestBody request: CreatePetRequest): ResponseEntity<PetResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(petService.createPet(request))
    }
}
```

이렇게 분리하는 이유: 공개/인증 엔드포인트가 한 클래스에 섞여 있으면 어떤 API가 인증이 필요한지 파악하기 어렵다. 파일명에 V0/V1이 붙으면 열지 않아도 역할이 드러난다.

## 4. JWT 인증 구조

### 토큰 종류

| 토큰 | 만료 | 용도 |
|------|------|------|
| Access Token | 30분 | API 요청 인증. `Authorization: Bearer {token}` 헤더 |
| Refresh Token | 7일 | Access Token 만료 시 갱신 |

### 핵심 클래스

| 클래스 | 위치 | 역할 |
|--------|------|------|
| `JwtProvider` | `config/jwt/` | 토큰 생성·검증·파싱 |
| `JwtFilter` | `config/jwt/` | 요청마다 Authorization 헤더에서 토큰 추출 후 SecurityContext에 인증 정보 설정 |
| `RefreshToken` | `domain/auth/entity/` | DB에 리프레시 토큰 저장 |
| `AuthService` | `domain/auth/service/` | 로그인, 토큰갱신, 로그아웃 비즈니스 로직 |

### 인증 흐름

1. 클라이언트가 `/api/v0/auth/login`으로 로그인 → Access + Refresh Token 응답
2. 이후 요청에 `Authorization: Bearer {accessToken}` 헤더 포함
3. JwtFilter가 토큰 검증 → SecurityContext에 인증 정보 설정
4. Access Token 만료 시 `/api/v0/auth/refresh`로 Refresh Token 전달 → 새 토큰 쌍 응답
5. 로그아웃 시 `/api/v1/auth/logout` → Refresh Token DB에서 삭제

### SecurityConfig 구조

```kotlin
@Configuration
@EnableWebSecurity
class SecurityConfig(private val jwtFilter: JwtFilter) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/api/v0/**").permitAll()
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                    .requestMatchers("/h2-console/**").permitAll()
                    .anyRequest().authenticated()
            }
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }
}
```

핵심: `/api/v0/**` 한 줄로 공개 API를 결정한다. 개별 URL을 나열하지 않는다.

## 5. 비밀번호 암호화

비밀번호는 반드시 BCrypt로 암호화한다. 평문 저장 금지.

```kotlin
// UserService에서 회원가입 시
password = passwordEncoder.encode(request.password)

// AuthService에서 로그인 시
require(passwordEncoder.matches(request.password, user.password))
```

## 6. 예외 처리 규칙

모든 예외는 `CustomException`을 사용한다. `IllegalArgumentException`, `IllegalStateException` 등 자바 기본 예외를 직접 던지지 않는다.

- 위치: `common/exception/CustomException.kt`
- `RuntimeException` 상속, `message`와 `HttpStatus`를 인자로 받음
- `GlobalExceptionHandler`에서 `CustomException`을 잡아서 해당 HTTP 상태코드로 응답

**사용법:**
```kotlin
// 404 - 리소스 없음
throw CustomException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND)

// 409 - 중복
throw CustomException("이미 사용 중인 이메일입니다.", HttpStatus.CONFLICT)

// 401 - 인증 실패
throw CustomException("이메일 또는 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED)

// 400 - 기본값 (status 생략 가능)
throw CustomException("잘못된 요청입니다.")
```

**상황별 HTTP 상태코드 기준:**

| 상황 | HttpStatus | 코드 |
|------|-----------|------|
| 리소스 없음 | NOT_FOUND | 404 |
| 중복 리소스 | CONFLICT | 409 |
| 인증 실패 | UNAUTHORIZED | 401 |
| 권한 없음 | FORBIDDEN | 403 |
| 잘못된 요청 | BAD_REQUEST | 400 |

## 7. TDD (테스트 주도 개발) 규칙

### 핵심 원칙

**API를 만들면 반드시 테스트를 함께 작성한다.** 테스트 없는 API는 머지하지 않는다.

`bootRun` 실행 시 Gradle이 자동으로 테스트를 먼저 수행한다. 테스트가 실패하면 서버가 올라가지 않는다. (`build.gradle`에 `bootRun.dependsOn('test')` 설정됨)

### 테스트 파일 위치

```
src/test/kotlin/org/grr/bridgy/
├── common/
│   └── BaseIntegrationTest.kt    ← 테스트 베이스 클래스
└── domain/
    └── {도메인}/
        └── {Domain}IntegrationTest.kt
```

### BaseIntegrationTest 상속

모든 통합 테스트는 `BaseIntegrationTest`를 상속받는다. 이 클래스가 제공하는 것:
- `mockMvc`, `objectMapper`, `jwtProvider`, `userRepository` 자동 주입
- `createTestUser()` — 테스트용 사용자 생성
- `accessTokenFor(user)` — JWT Access Token 생성
- `withAuth(user)` — 요청에 Authorization 헤더 추가 (V1 테스트용)
- `toJson(obj)` — JSON 직렬화

### 테스트 작성 패턴

```kotlin
@TestMethodOrder(OrderAnnotation::class)
class NewDomainIntegrationTest : BaseIntegrationTest() {

    @Autowired lateinit var newDomainRepository: NewDomainRepository

    // ─── V0 (공개) API 테스트 ───

    @Test
    @Order(1)
    fun `V0 목록 조회`() {
        // Given: 테스트 데이터 준비
        // When: V0 엔드포인트 호출 (인증 없이)
        mockMvc.perform(get("/api/v0/new-domain"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(expectedCount))
    }

    // ─── V1 (인증) API 테스트 ───

    @Test
    @Order(2)
    fun `V1 생성 성공`() {
        val user = createTestUser()
        val request = CreateNewDomainRequest(...)

        // When: V1 엔드포인트 호출 (인증 포함)
        mockMvc.perform(
            post("/api/v1/new-domain")
                .withAuth(user)                           // ← JWT 인증 헤더 자동 추가
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isCreated)
    }

    @Test
    @Order(3)
    fun `V1 생성 실패 - 인증 없음`() {
        // V1 엔드포인트에 토큰 없이 접근 → 401/403 확인
        mockMvc.perform(
            post("/api/v1/new-domain")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
        )
            .andExpect(status().isUnauthorized.or(status().isForbidden))
    }

    @Test
    fun `트랜잭션 롤백 검증`() {
        Assertions.assertEquals(0, newDomainRepository.count())
    }
}
```

### 테스트 필수 항목 체크리스트

새 API를 만들 때 아래 테스트를 반드시 포함한다:

1. **성공 케이스**: 정상 요청 → 기대 응답 + DB 반영 확인
2. **실패 케이스 - 리소스 없음**: 존재하지 않는 ID → 404 NOT_FOUND
3. **실패 케이스 - 중복**: 중복 데이터 → 409 CONFLICT (해당되는 경우)
4. **실패 케이스 - 인증 없음**: V1 API에 토큰 없이 접근 → 401/403
5. **트랜잭션 롤백 검증**: 이전 테스트 데이터가 남아있지 않은지 확인

### 테스트 설정

테스트 환경 설정은 `src/test/resources/application-test.yml`:
- H2 인메모리 DB (`create-drop`)
- Redis 비활성화
- 테스트용 JWT 시크릿 키

### 테스트 명명 규칙

```kotlin
// 패턴: `{V버전} {기능} {성공/실패} - {조건}`
fun `V0 전체 목록 조회`()
fun `V1 등록 성공`()
fun `V1 등록 실패 - 인증 없음`()
fun `V1 수정 실패 - 존재하지 않는 ID`()
```

## 8. 패키지 구조

```
org.grr.bridgy/
├── common/
│   ├── config/
│   │   ├── SecurityConfig.kt
│   │   ├── WebConfig.kt
│   │   ├── RedisConfig.kt
│   │   ├── SwaggerConfig.kt
│   │   └── GlobalExceptionHandler.kt
│   ├── exception/
│   │   ├── CustomException.kt
│   │   └── ErrorResponse.kt
│   ├── jwt/
│   │   ├── JwtProvider.kt
│   │   └── JwtFilter.kt
│   └── BaseTime.kt
├── domain/
│   ├── auth/
│   │   ├── controller/
│   │   │   ├── AuthControllerV0.kt
│   │   │   └── AuthControllerV1.kt
│   │   ├── dto/
│   │   ├── entity/
│   │   ├── repository/
│   │   └── service/
│   └── {도메인}/
│       ├── controller/
│       │   ├── {Domain}ControllerV0.kt
│       │   └── {Domain}ControllerV1.kt
│       ├── dto/
│       ├── entity/
│       ├── repository/
│       └── service/
└── BridgyApplication.kt
```

## 9. 새 도메인 추가 체크리스트

새로운 도메인(예: `notification`)을 추가할 때:

1. **엔티티**: `domain/{도메인}/entity/` 에 생성, `BaseTime()` 상속
2. **Repository**: `domain/{도메인}/repository/` 에 JpaRepository 인터페이스
3. **DTO**: `domain/{도메인}/dto/` 에 Request/Response DTO
4. **Service**: `domain/{도메인}/service/` 에 비즈니스 로직, 예외는 `CustomException` 사용
5. **Controller V0**: 공개 조회 API가 있으면 `{Domain}ControllerV0.kt` 생성
6. **Controller V1**: 인증 필요 API가 있으면 `{Domain}ControllerV1.kt` 생성
7. **테스트**: `{Domain}IntegrationTest.kt` 생성, `BaseIntegrationTest` 상속, V0/V1 모두 테스트
8. **SecurityConfig 수정 불필요**: v0/v1 경로 규칙을 따르면 자동으로 인증 여부가 결정됨

## 10. 기술 스택

- Kotlin 2.0+, JDK 21
- Virtual Threads 활성화 (`spring.threads.virtual.enabled: true`)
- Spring Boot 3.5+
- Spring Data JPA + Hibernate
- PostgreSQL (프로덕션), H2 (로컬/테스트)
- Redis (캐싱)
- JWT: io.jsonwebtoken:jjwt 0.12+
- Spring Security (Stateless, BCrypt)
- Swagger: springdoc-openapi
- Jackson: SNAKE_CASE 전략
- Kafka: spring-kafka (KRaft mode, no ZooKeeper)

## 11. 운영 안전성 체크리스트

새 기능을 추가하거나 코드 리뷰 시 반드시 아래 항목을 확인한다.

### 11-1. DB 인덱스
모든 JPA 엔티티에서 조회/필터에 자주 쓰이는 컬럼에 `@Index`를 선언한다.

```kotlin
@Table(name = "pets", indexes = [
    Index(name = "idx_pet_user_id", columnList = "user_id"),
    Index(name = "idx_pet_species",  columnList = "species")
])
```

기준:
- FK 컬럼(`user_id`, `pet_id` 등)은 반드시 인덱스
- WHERE/ORDER BY에 사용되는 컬럼도 인덱스
- `email`, `nickname` 같이 고유성 검색에 쓰이는 컬럼도 인덱스

### 11-2. 페이징 필수
`List<T>` 반환 API는 데이터가 늘어날수록 OOM, 느린 쿼리의 원인이 된다. 공개 목록 조회 API는 반드시 `Page<T>` + `Pageable`로 만든다.

```kotlin
// 서비스
fun getAllPets(pageable: Pageable): Page<PetResponse> {
    val safePageable = capPageSize(pageable)   // MAX_PAGE_SIZE로 상한 제한
    return petRepository.findAll(safePageable).map { PetResponse.from(it) }
}

// 컨트롤러
@GetMapping
fun getAllPets(@RequestParam(defaultValue = "0") page: Int,
              @RequestParam(defaultValue = "20") size: Int): ResponseEntity<Page<PetResponse>> {
    val pageable = PageRequest.of(page, size.coerceAtMost(FreeTierLimits.MAX_PAGE_SIZE))
    return ResponseEntity.ok(petService.getAllPets(pageable))
}
```

`capPageSize()` 헬퍼를 서비스에 두어 클라이언트가 임의로 size를 크게 요청해도 상한에 걸리게 한다.

### 11-3. JPQL JOIN 문법
Spring Data JPA JPQL은 `JOIN ... ON` 문법을 지원하지 않는다. 엔티티 연관관계가 없는 컬럼 기준 조인은 서브쿼리로 대체한다.

```kotlin
// 잘못된 예 (컴파일 오류)
@Query("SELECT p FROM Pet p LEFT JOIN Like l ON l.pet.id = p.id ORDER BY COUNT(l) DESC")

// 올바른 예
@Query("SELECT p FROM Pet p ORDER BY (SELECT COUNT(l) FROM Like l WHERE l.pet = p) DESC")
```

### 11-4. 캐시 적중 (Cache Hit-through)
서비스 A가 서비스 B의 `@Cacheable` 메서드를 직접 호출해야 캐시가 적중한다.
같은 서비스 내 메서드 호출은 프록시를 거치지 않아 캐시가 동작하지 않는다.

- `PetService.toDashboardResponse()`에서 likeCount/commentCount/galleryCount를 구할 때 각 서비스의 `getLikeCount()`, `getCommentCount()`, `getPhotoCount()`를 호출한다.
- 동일 클래스 내 `@Cacheable` 메서드를 `this.xxx()`로 호출하면 캐시가 무시된다.

### 11-5. 카운트 캐시 & 이중 퇴거 (Dual Eviction)
카운트(좋아요 수, 댓글 수, 사진 수)는 별도 캐시 키로 관리하고, 데이터 변경 시 목록 캐시와 카운트 캐시를 함께 퇴거한다.

```kotlin
@Transactional
@Caching(evict = [
    CacheEvict("gallery", key = "#request.petId"),
    CacheEvict("galleryCounts", key = "#request.petId")
])
fun addPhoto(request: CreateGalleryRequest): GalleryResponse { ... }

@Cacheable("galleryCounts", key = "#petId")
fun getPhotoCount(petId: Long): Long = galleryRepository.countByPetId(petId)
```

캐시별 TTL 기준 (RedisConfig):
- `pets`, `petsByUser`: 15~30분
- `gallery`, `healthRecords`: 10분
- `dailyRecords`: 5분
- `galleryCounts`: 5분
- `likeCounts`, `commentCounts`: 1분 (변경 빈도가 높으므로 짧게)

### 11-6. HikariCP 커넥션 풀
`database.yml`에 반드시 HikariCP 설정을 포함한다. 기본값 그대로 두면 커넥션이 부족하거나 너무 많이 열릴 수 있다.

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20     # CPU 코어수 × 2 기준, 트래픽에 맞게 조정
      minimum-idle: 5
      idle-timeout: 600000      # 10분
      connection-timeout: 30000  # 30초
      max-lifetime: 1800000     # 30분
```

### 11-7. Redis ↔ DB 하이브리드 (RefreshToken)
RefreshToken은 Redis를 1차, DB를 2차 저장소로 사용한다. Redis 장애 시에도 DB로 폴백하여 사용자가 재로그인하지 않아도 된다.

- `RefreshTokenRedisRepository`: `@Profile("!test")`로 테스트 환경에서 제외
- `AuthService`: `Optional<RefreshTokenRedisRepository>` 주입으로 테스트 환경 호환
- Redis 미스 시 DB에서 조회 후 남은 TTL 계산하여 Redis에 재저장

### 11-8. 테스트 환경 분리 패턴
Redis, Kafka 등 외부 인프라 의존 빈은 `@Profile("!test")`로 테스트 환경에서 제외하고, 해당 빈을 사용하는 서비스에서는 `Optional<T>` 주입 패턴을 사용한다.

```kotlin
// 빈 정의
@Component
@Profile("!test")
class RefreshTokenRedisRepository(...) { ... }

// 주입 (AuthService)
class AuthService(
    private val redisRepo: Optional<RefreshTokenRedisRepository>
) {
    private fun saveToRedis(...) {
        redisRepo.ifPresent { it.save(...) }
    }
}
```
