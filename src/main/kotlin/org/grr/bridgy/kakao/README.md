# kakao 패키지

카카오톡 채널 연동 모듈입니다. 카카오 i 오픈빌더 스킬 서버로 동작하며 고객 메시지를 수신하고 AI 응답을 반환합니다.

## 구조

```
kakao/
├── controller/
│   └── KakaoWebhookController.kt   # 웹훅 엔드포인트
├── dto/
│   └── KakaoDto.kt                 # 카카오 스킬 v2.0 요청/응답 DTO
└── service/
    └── KakaoChannelService.kt      # 메시지 처리 핵심 로직
```

## 카카오 스킬 v2.0 포맷

### 요청 (KakaoWebhookRequest)

```json
{
  "userRequest": {
    "user": { "id": "카카오_유저_ID" },
    "utterance": "내일 저녁 6시 2명 예약할게요"
  }
}
```

### 응답 (KakaoSkillResponse)

```json
{
  "version": "2.0",
  "template": {
    "outputs": [
      { "simpleText": { "text": "응답 메시지" } }
    ]
  }
}
```

`kakaoTextResponse()` 유틸 함수로 간편하게 응답을 생성합니다.

## KakaoChannelService 핵심 플로우

```
카카오 웹훅 수신
  ↓
1. 고객 자동 식별/생성 (kakaoUserId 기반, 회원가입 불필요)
  ↓
2. 매장 조회 (없으면 에러 응답)
  ↓
3. AiService.classifyIntent() → 메시지 의도 분류
  ↓
4. 고객 메시지 DB 저장 (ChatMessage)
  ↓
5. 의도별 처리
   ├─ RESERVATION → 자연어 파싱 → 예약 자동 생성 → ReservationEvent 발행
   └─ 그 외 → AI 응답 생성
  ↓
6. AI 응답 DB 저장 (ChatMessage)
  ↓
7. NotificationEvent 발행 (사장님 알림)
  ↓
8. 카카오 스킬 응답 반환
```

## API 엔드포인트

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/kakao/webhook/{storeId}` | 카카오톡 메시지 수신 (스킬 서버) |
| GET | `/api/kakao/webhook/health` | 헬스체크 (스킬 등록 시 사용) |

## 카카오 오픈빌더 연동 방법

1. 카카오 i 오픈빌더에서 스킬 생성
2. URL: `https://{도메인}/api/kakao/webhook/{storeId}`
3. 시나리오 블록에 스킬 연결
4. 채널 배포

## 관련 테스트

- `KakaoChannelServiceTest.kt` (7개): 일반 문의, 신규 고객, 매장 미존재, 예약 자동 생성, 불완전 예약, 응답 포맷
- `KakaoWebhookControllerTest.kt` (5개): 웹훅 API 테스트
