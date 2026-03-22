# Bridgy

소상공인 매장과 고객을 카카오톡으로 연결하는 AI 에이전트 플랫폼입니다.

고객은 앱 설치나 회원가입 없이 카카오톡으로 문의하면 AI가 자동 응답하고, 사장님은 웹 대시보드에서 매장을 관리합니다.

## 기술 스택

- **Language**: Kotlin 2.0.0
- **Framework**: Spring Boot 3.2.5 (JDK 17)
- **Database**: PostgreSQL 16 (운영) / H2 (로컬/테스트)
- **Cache**: Redis 7
- **Message Broker**: Apache Kafka (Confluent 7.6.0)
- **ORM**: Spring Data JPA
- **API Docs**: SpringDoc OpenAPI 2.5.0
- **Test**: JUnit 5 + Mockito-Kotlin (82개 테스트)
- **Infra**: Docker Compose

## 프로젝트 구조

```
src/main/kotlin/org/grr/bridgy/
├── ai/           # AI 서비스 (의도 분류, 자연어 파싱, 응답 생성)
├── config/       # Redis, Kafka, Security, CORS 설정
├── domain/
│   ├── chat/         # 카카오톡 대화 내역
│   ├── customer/     # 고객 (카카오 유저 자동 식별)
│   ├── reservation/  # 예약 관리
│   ├── review/       # 리뷰 + AI 자동 답글
│   └── store/        # 매장 관리
├── kafka/        # 이벤트 발행/소비 (Producer, Consumer, Event)
└── kakao/        # 카카오톡 채널 웹훅 연동
```

각 패키지별 상세 내용은 해당 디렉토리의 `README.md`를 참조하세요.

## 시작하기

### 1. 인프라 실행

```bash
docker-compose up -d
```

PostgreSQL(5432), Redis(6379), Kafka(9092), Kafka UI(8090)가 실행됩니다.

### 2. 설정 파일 준비

```bash
cp src/main/resources/application.yml.example src/main/resources/application.yml
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
```

`.yml` 파일은 `.gitignore`에 등록되어 있으므로 `.example` 파일을 복사하여 사용합니다.

### 3. 애플리케이션 실행

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

### 4. API 문서 확인

```
http://localhost:8080/swagger-ui/index.html
```

## 핵심 플로우

```
고객 (카카오톡) → 웹훅 → AI 의도 분류 → 자동 응답
                                  ↓
                         예약 의도 → 자연어 파싱 → 예약 자동 생성
                                  ↓
                         Kafka 이벤트 → 사장님 알림
```

## 테스트

```bash
./gradlew test
```

총 82개 테스트 (11개 테스트 파일)가 TDD로 작성되어 있습니다.

## GitHub

```
https://github.com/choiKYeon/bridgy
```
