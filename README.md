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
                    ┌─────────────────────────┐
                    │          :app           │
                    │  (부트스트랩, DI 조립)   │
                    └───────────┬─────────────┘
                                │
        ┌───────────────────────┼───────────────────────┐
        │                       │                       │
        ▼                       ▼                       ▼
┌───────────────┐      ┌───────────────┐      ┌───────────────┐
│  :adapter-in  │      │ :application  │      │ :adapter-out  │
│  (외부 → 앱)  │      │   (UseCase)   │      │  (앱 → 외부)  │
├───────────────┤      ├───────────────┤      ├───────────────┤
│ • Controller  │      │ • Service     │      │ • API Client  │
│ • Scheduler   │      │               │      │ • JPA         │
│               │      │               │      │ • FCM         │
└───────┬───────┘      └───────┬───────┘      └───────┬───────┘
        │                      │                      │
        └──────────────────────┴──────────────────────┘
                               │
                               ▼
                      ┌───────────────┐
                      │   :domain     │
                      │  (Port/Model) │
                      └───────────────┘
```

### 모듈별 역할 및 의존성

| 모듈 | 역할 | 설명 |
|------|------|------|
| `:domain` | Port 인터페이스, 도메인 모델 | 순수 Kotlin, 의존성 없음 |
| `:application` | UseCase 구현 | 비즈니스 로직 |
| `:adapter-in` | 외부 → 앱 | Controller, Scheduler |
| `:adapter-out` | 앱 → 외부 | API 클라이언트, DB, FCM |
| `:app` | 부트스트랩 | 설정, DI 조립 |

### 의존성 규칙 (컴파일 타임 강제)

```
✅ adapter-in  → domain (Controller가 UseCase 호출)
✅ adapter-out → domain (Adapter가 Port 구현)
✅ application → domain (Service가 Port 사용)
❌ adapter-in  → adapter-out (컴파일 에러!)
❌ application → adapter-* (컴파일 에러!)
```

### 디렉토리 구조

> 📁 클릭하면 해당 파일로 이동

**[:domain](domain/)** — 도메인 모듈 (Port + Model)
- [`model/`](domain/src/main/kotlin/com/seriouschoi/aircheck/domain/model/)
  - [`AirQuality.kt`](domain/src/main/kotlin/com/seriouschoi/aircheck/domain/model/AirQuality.kt)
  - [`Weather.kt`](domain/src/main/kotlin/com/seriouschoi/aircheck/domain/model/Weather.kt)
- [`port/in/`](domain/src/main/kotlin/com/seriouschoi/aircheck/domain/port/in/) — 인바운드 포트 (UseCase 인터페이스)
  - [`GetWeatherUseCase.kt`](domain/src/main/kotlin/com/seriouschoi/aircheck/domain/port/in/GetWeatherUseCase.kt)
  - [`PushSubscriptionUseCase.kt`](domain/src/main/kotlin/com/seriouschoi/aircheck/domain/port/in/PushSubscriptionUseCase.kt)
- [`port/out/`](domain/src/main/kotlin/com/seriouschoi/aircheck/domain/port/out/) — 아웃바운드 포트
  - [`WeatherPort.kt`](domain/src/main/kotlin/com/seriouschoi/aircheck/domain/port/out/WeatherPort.kt)
  - [`AirQualityPort.kt`](domain/src/main/kotlin/com/seriouschoi/aircheck/domain/port/out/AirQualityPort.kt)
  - [`PushNotificationPort.kt`](domain/src/main/kotlin/com/seriouschoi/aircheck/domain/port/out/PushNotificationPort.kt)
  - [`PushSubscriptionPort.kt`](domain/src/main/kotlin/com/seriouschoi/aircheck/domain/port/out/PushSubscriptionPort.kt)

**[:application](application/)** — 애플리케이션 모듈 (UseCase 구현)
- [`WeatherService.kt`](application/src/main/kotlin/com/seriouschoi/aircheck/application/WeatherService.kt)
- [`PushSubscriptionService.kt`](application/src/main/kotlin/com/seriouschoi/aircheck/application/PushSubscriptionService.kt)

**[:adapter-in](adapter-in/)** — 인바운드 어댑터 (외부 → 앱)
- [`in/web/`](adapter-in/src/main/kotlin/com/seriouschoi/aircheck/adapter/in/web/) — REST API (사용자 호출)
  - [`WeatherController.kt`](adapter-in/src/main/kotlin/com/seriouschoi/aircheck/adapter/in/web/WeatherController.kt)
  - [`PushController.kt`](adapter-in/src/main/kotlin/com/seriouschoi/aircheck/adapter/in/web/PushController.kt)
- [`in/scheduler/`](adapter-in/src/main/kotlin/com/seriouschoi/aircheck/adapter/in/scheduler/) — 스케줄러
  - [`CacheRefreshScheduler.kt`](adapter-in/src/main/kotlin/com/seriouschoi/aircheck/adapter/in/scheduler/CacheRefreshScheduler.kt)
  - [`PushScheduler.kt`](adapter-in/src/main/kotlin/com/seriouschoi/aircheck/adapter/in/scheduler/PushScheduler.kt)

**[:adapter-out](adapter-out/)** — 아웃바운드 어댑터 (앱 → 외부)
- [`out/api/`](adapter-out/src/main/kotlin/com/seriouschoi/aircheck/adapter/out/api/) — 외부 API 호출
  - [`OpenMeteoAdapter.kt`](adapter-out/src/main/kotlin/com/seriouschoi/aircheck/adapter/out/api/OpenMeteoAdapter.kt) — 날씨 API
  - [`AirKoreaAdapter.kt`](adapter-out/src/main/kotlin/com/seriouschoi/aircheck/adapter/out/api/AirKoreaAdapter.kt) — 미세먼지 API
  - [`FcmAdapter.kt`](adapter-out/src/main/kotlin/com/seriouschoi/aircheck/adapter/out/api/FcmAdapter.kt) — 푸시 알림
- [`out/persistence/`](adapter-out/src/main/kotlin/com/seriouschoi/aircheck/adapter/out/persistence/) — DB 접근
  - [`PushSubscriptionEntity.kt`](adapter-out/src/main/kotlin/com/seriouschoi/aircheck/adapter/out/persistence/PushSubscriptionEntity.kt)
  - [`PushSubscriptionRepository.kt`](adapter-out/src/main/kotlin/com/seriouschoi/aircheck/adapter/out/persistence/PushSubscriptionRepository.kt)
  - [`PushSubscriptionAdapter.kt`](adapter-out/src/main/kotlin/com/seriouschoi/aircheck/adapter/out/persistence/PushSubscriptionAdapter.kt)

**[:app](app/)** — 부트스트랩 모듈
- [`AircheckServerApplication.kt`](app/src/main/kotlin/com/seriouschoi/aircheck/AircheckServerApplication.kt)
- [`config/`](app/src/main/kotlin/com/seriouschoi/aircheck/config/)
  - [`CacheConfig.kt`](app/src/main/kotlin/com/seriouschoi/aircheck/config/CacheConfig.kt)

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

## 왜 이렇게 나눴나?

### 1. adapter-in / adapter-out 분리

| 모듈 | 방향 | 예시 |
|------|------|------|
| adapter-in | 외부 → 앱 | REST API, Scheduler |
| adapter-out | 앱 → 외부 | 공공 API, DB, FCM |

**장점**: 날씨 API 바뀌면 `adapter-out`만 수정!

### 2. 컴파일 타임 의존성 강제

```kotlin
// adapter-in에서 adapter-out import 불가능!
import com.seriouschoi.aircheck.adapter.out.api.AirKoreaAdapter  // ❌ 컴파일 에러
```

### 3. 모듈 교체 용이

```kotlin
// build.gradle.kts
dependencies {
    // implementation(project(":adapter-out"))  // Open-Meteo
    implementation(project(":adapter-out-kma")) // 기상청으로 교체!
}
```
