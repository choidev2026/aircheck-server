# AirCheck Server 🌤️

에어체크 앱의 백엔드 서버

## 기술 스택

- **Spring Boot 3.4.4** + Kotlin
- **MariaDB** (사용자 데이터)
- **Caffeine Cache** (API 응답 캐시)
- **Firebase Admin SDK** (FCM 푸시)
- **OkHttp** (HTTP 클라이언트)

## 아키텍처

### 헥사고날 (Port-Adapter) 아키텍처

```
┌─────────────────────────────────────────────────────────────────┐
│                      Adapter (In)                                │
│  ┌─────────────────┐  ┌─────────────────┐                       │
│  │  Web Controller │  │    Scheduler    │                       │
│  └────────┬────────┘  └────────┬────────┘                       │
└───────────┼─────────────────────┼───────────────────────────────┘
            │                     │
            ▼                     ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Domain (Port In)                             │
│  ┌─────────────────┐  ┌─────────────────────┐                   │
│  │ GetWeatherUseCase│  │PushSubscriptionUseCase│                │
│  │   (interface)   │  │     (interface)      │                  │
│  └────────┬────────┘  └──────────┬───────────┘                  │
└───────────┼──────────────────────┼──────────────────────────────┘
            │                      │
            ▼                      ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Application                                  │
│  ┌─────────────────┐  ┌─────────────────────┐                   │
│  │  WeatherService │  │PushSubscriptionService│  ← Use Case 구현 │
│  └────────┬────────┘  └──────────┬───────────┘                  │
└───────────┼──────────────────────┼──────────────────────────────┘
            │                      │
            ▼                      ▼
┌─────────────────────────────────────────────────────────────────┐
│                     Domain (Port Out)                            │
│  ┌───────────┐ ┌──────────────┐ ┌──────────────────┐            │
│  │WeatherPort│ │AirQualityPort│ │PushNotificationPort│           │
│  │(interface)│ │  (interface) │ │    (interface)    │           │
│  └─────┬─────┘ └──────┬───────┘ └────────┬──────────┘           │
└────────┼──────────────┼──────────────────┼──────────────────────┘
         │              │                  │
         ▼              ▼                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Adapter (Out)                               │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐             │
│  │OpenMeteoAdapter│ │AirKoreaAdapter│ │  FcmAdapter  │            │
│  └──────────────┘ └──────────────┘ └──────────────┘             │
│                                                                  │
│  ┌──────────────────────────────┐                               │
│  │  PushSubscriptionRepository  │  (JPA)                        │
│  └──────────────────────────────┘                               │
└─────────────────────────────────────────────────────────────────┘
```

### 디렉토리 구조

```
src/main/kotlin/com/seriouschoi/aircheck/
├── AircheckServerApplication.kt
│
├── domain/                          # 도메인 레이어
│   ├── model/                       # 도메인 모델
│   │   ├── AirQuality.kt
│   │   └── Weather.kt
│   └── port/
│       ├── in/                      # 인바운드 포트 (Use Case)
│       │   ├── GetWeatherUseCase.kt
│       │   └── PushSubscriptionUseCase.kt
│       └── out/                     # 아웃바운드 포트
│           ├── WeatherPort.kt
│           ├── AirQualityPort.kt
│           └── PushNotificationPort.kt
│
├── application/                     # 애플리케이션 레이어
│   ├── WeatherService.kt            # Use Case 구현
│   └── PushSubscriptionService.kt
│
├── adapter/                         # 어댑터 레이어
│   ├── in/                          # 인바운드 어댑터
│   │   ├── web/
│   │   │   ├── WeatherController.kt
│   │   │   └── PushController.kt
│   │   └── scheduler/
│   │       ├── CacheRefreshScheduler.kt
│   │       └── PushScheduler.kt
│   └── out/                         # 아웃바운드 어댑터
│       ├── api/
│       │   ├── OpenMeteoAdapter.kt
│       │   ├── AirKoreaAdapter.kt
│       │   └── FcmAdapter.kt
│       └── persistence/
│           ├── PushSubscriptionEntity.kt
│           └── PushSubscriptionRepository.kt
│
└── config/
    └── CacheConfig.kt
```

### 아키텍처 장점

1. **외부 API 교체 용이**
   - 기상청 API로 바꾸려면? `WeatherPort` 구현체만 추가
   - 에어코리아 → 다른 API? `AirQualityPort` 구현체 교체

2. **테스트 용이**
   - Port를 Mock으로 교체하여 단위 테스트

3. **의존성 역전**
   - Domain이 외부 의존성 없이 순수 비즈니스 로직만

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

### Docker Compose (권장)
```bash
export AIRKOREA_API_KEY=your-api-key
export FIREBASE_CREDENTIALS_JSON='{"type":"service_account",...}'
docker-compose up -d
```

### 로컬 실행
```bash
docker run -d --name mariadb \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=aircheck \
  -e MYSQL_USER=aircheck \
  -e MYSQL_PASSWORD=aircheck \
  -p 3306:3306 \
  mariadb:11

export AIRKOREA_API_KEY=xxx
export DB_HOST=localhost
./gradlew bootRun
```

## TODO

- [ ] AWS 배포
- [ ] GitHub Actions CI/CD
- [ ] 앱 연동
- [ ] 테스트 코드
