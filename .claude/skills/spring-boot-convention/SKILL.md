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

## 6. 패키지 구조

```
org.grr.bridgy/
├── config/
│   ├── SecurityConfig.kt
│   ├── WebConfig.kt
│   ├── RedisConfig.kt
│   └── jwt/
│       ├── JwtProvider.kt
│       └── JwtFilter.kt
├── domain/
│   ├── common/
│   │   └── BaseTime.kt
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

## 7. 새 도메인 추가 체크리스트

새로운 도메인(예: `notification`)을 추가할 때:

1. **엔티티**: `domain/{도메인}/entity/` 에 생성, `BaseTime()` 상속
2. **Repository**: `domain/{도메인}/repository/` 에 JpaRepository 인터페이스
3. **DTO**: `domain/{도메인}/dto/` 에 Request/Response DTO
4. **Service**: `domain/{도메인}/service/` 에 비즈니스 로직
5. **Controller V0**: 공개 조회 API가 있으면 `{Domain}ControllerV0.kt` 생성
6. **Controller V1**: 인증 필요 API가 있으면 `{Domain}ControllerV1.kt` 생성
7. **SecurityConfig 수정 불필요**: v0/v1 경로 규칙을 따르면 자동으로 인증 여부가 결정됨

## 8. 기술 스택

- Kotlin 2.0+, JDK 17
- Spring Boot 3.5+
- Spring Data JPA + Hibernate
- PostgreSQL (프로덕션), H2 (로컬/테스트)
- Redis (캐싱)
- JWT: io.jsonwebtoken:jjwt 0.12+
- Spring Security (Stateless, BCrypt)
- Swagger: springdoc-openapi
- Jackson: SNAKE_CASE 전략
