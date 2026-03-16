# AirCheck Server 🌤️

에어체크 앱의 백엔드 서버

## 기술 스택

- **Spring Boot 3.4.4** + Kotlin
- **MariaDB** (사용자 데이터)
- **Caffeine Cache** (API 응답 캐시)
- **Firebase Admin SDK** (FCM 푸시)
- **멀티모듈** + 헥사고날 아키텍처

## 아키텍처

### 멀티모듈 + 헥사고날 (Port-Adapter)

```
┌─────────────────────────────────────────────────────────────────┐
│                         :app                                     │
│              (부트스트랩, 설정, 의존성 조립)                      │
└───────────────────────────┬─────────────────────────────────────┘
                            │ depends on
            ┌───────────────┼───────────────┐
            ▼               ▼               ▼
┌───────────────┐  ┌───────────────┐  ┌───────────────┐
│   :adapter    │  │ :application  │  │   :domain     │
│               │  │               │  │               │
│ ┌───────────┐ │  │ ┌───────────┐ │  │ ┌───────────┐ │
│ │Controller │ │  │ │  Service  │ │  │ │   Model   │ │
│ │ Scheduler │ │  │ │ (UseCase  │ │  │ │   Port    │ │
│ │   API     │ │  │ │   Impl)   │ │  │ │(interface)│ │
│ │   JPA     │ │  │ └───────────┘ │  │ └───────────┘ │
│ └───────────┘ │  └───────┬───────┘  └───────▲───────┘
└───────┬───────┘          │                  │
        │                  │                  │
        └──────────────────┴──────────────────┘
                    depends on :domain only
```

### 모듈별 역할 및 의존성

| 모듈 | 역할 | 의존성 |
|------|------|--------|
| `:domain` | 도메인 모델, Port 인터페이스 | 없음 (순수 Kotlin) |
| `:application` | UseCase 구현 | `:domain` |
| `:adapter` | Controller, API 클라이언트, JPA | `:domain` |
| `:app` | 부트스트랩, 설정, DI 조립 | 모든 모듈 |

### 의존성 규칙 (컴파일 타임 강제)

```
✅ application → domain (UseCase가 Port 인터페이스 사용)
✅ adapter → domain (Adapter가 Port 인터페이스 구현)
❌ application → adapter (컴파일 에러!)
❌ domain → 아무것도 (순수)
```

### 디렉토리 구조

```
aircheck-server/
├── domain/                         # 도메인 모듈
│   └── src/main/kotlin/.../domain/
│       ├── model/
│       │   ├── AirQuality.kt
│       │   └── Weather.kt
│       └── port/
│           ├── in/                 # 인바운드 포트 (UseCase)
│           │   ├── GetWeatherUseCase.kt
│           │   └── PushSubscriptionUseCase.kt
│           └── out/                # 아웃바운드 포트
│               ├── WeatherPort.kt
│               ├── AirQualityPort.kt
│               ├── PushNotificationPort.kt
│               └── PushSubscriptionPort.kt
│
├── application/                    # 애플리케이션 모듈
│   └── src/main/kotlin/.../application/
│       ├── WeatherService.kt       # GetWeatherUseCase 구현
│       └── PushSubscriptionService.kt
│
├── adapter/                        # 어댑터 모듈
│   └── src/main/kotlin/.../adapter/
│       ├── in/
│       │   ├── web/
│       │   │   ├── WeatherController.kt
│       │   │   └── PushController.kt
│       │   └── scheduler/
│       │       ├── CacheRefreshScheduler.kt
│       │       └── PushScheduler.kt
│       └── out/
│           ├── api/
│           │   ├── OpenMeteoAdapter.kt
│           │   ├── AirKoreaAdapter.kt
│           │   └── FcmAdapter.kt
│           └── persistence/
│               ├── PushSubscriptionEntity.kt
│               ├── PushSubscriptionRepository.kt
│               └── PushSubscriptionAdapter.kt
│
└── app/                            # 부트스트랩 모듈
    └── src/main/kotlin/.../
        ├── AircheckServerApplication.kt
        └── config/
            └── CacheConfig.kt
```

## API 엔드포인트

### 날씨/대기질

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/v1/weather?lat={}&lng={}` | 통합 (날씨+대기질) |
| GET | `/api/v1/weather/forecast?lat={}&lng={}` | 날씨만 |
| GET | `/api/v1/weather/air?lat={}&lng={}` | 대기질만 |

### 푸시 알림

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/v1/push/subscribe` | 푸시 구독 |
| POST | `/api/v1/push/unsubscribe` | 구독 해제 |
| POST | `/api/v1/push/enabled` | 알림 on/off |

## 환경 변수

| 변수 | 설명 | 필수 |
|------|------|------|
| `AIRKOREA_API_KEY` | 에어코리아 API 키 | ✅ |
| `FIREBASE_CREDENTIALS_JSON` | Firebase 서비스 계정 JSON | ✅ (푸시용) |
| `DB_HOST` | MariaDB 호스트 | ✅ |
| `DB_PORT` | MariaDB 포트 (기본: 3306) | |
| `DB_NAME` | 데이터베이스 이름 | ✅ |
| `DB_USER` | DB 사용자 | ✅ |
| `DB_PASSWORD` | DB 비밀번호 | ✅ |

## 실행

### Docker Compose
```bash
export AIRKOREA_API_KEY=your-api-key
export FIREBASE_CREDENTIALS_JSON='{"type":"service_account",...}'
docker-compose up -d
```

### 로컬 빌드
```bash
./gradlew :app:bootJar
java -jar app/build/libs/app-0.0.1-SNAPSHOT.jar
```

## 왜 멀티모듈인가?

1. **의존성 컴파일 타임 강제**
   - adapter에서 application 클래스 import 불가능
   - 실수로 아키텍처 위반 → 빌드 에러

2. **명확한 경계**
   - 각 모듈의 역할이 명확
   - 코드 리뷰 시 "이 코드가 여기 있어도 되나?" 판단 쉬움

3. **빌드 최적화**
   - 변경된 모듈만 재빌드
   - 대규모 프로젝트에서 빌드 시간 단축
