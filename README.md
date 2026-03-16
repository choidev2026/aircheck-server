# AirCheck Server 🌤️

에어체크 앱의 백엔드 서버

## 기술 스택

- **Spring Boot 3.4.4** + Kotlin
- **MariaDB** (사용자 데이터)
- **Caffeine Cache** (API 응답 캐시)
- **Firebase Admin SDK** (FCM 푸시)
- **OkHttp** (HTTP 클라이언트)

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

#### 푸시 구독 요청
```json
POST /api/v1/push/subscribe
{
  "fcmToken": "device-fcm-token",
  "latitude": 37.5665,
  "longitude": 126.9780,
  "address": "서울특별시 중구",
  "pushTimeHour": 7,
  "pushTimeMinute": 0,
  "enabled": true
}
```

### 헬스체크
```
GET /actuator/health
```

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
# 환경변수 설정
export AIRKOREA_API_KEY=your-api-key
export FIREBASE_CREDENTIALS_JSON='{"type":"service_account",...}'

# 실행
docker-compose up -d
```

### 로컬 실행 (MariaDB 필요)
```bash
# MariaDB 실행
docker run -d --name mariadb \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=aircheck \
  -e MYSQL_USER=aircheck \
  -e MYSQL_PASSWORD=aircheck \
  -p 3306:3306 \
  mariadb:11

# 서버 실행
export AIRKOREA_API_KEY=xxx
export DB_HOST=localhost
./gradlew bootRun
```

## 프로젝트 구조

```
src/main/kotlin/com/seriouschoi/aircheck/
├── AircheckServerApplication.kt
├── config/
│   └── CacheConfig.kt
├── controller/
│   ├── WeatherController.kt      # 날씨/대기질 API
│   └── PushController.kt         # 푸시 알림 API
├── entity/
│   └── PushSubscription.kt       # 푸시 구독 엔티티
├── model/
│   ├── AirQuality.kt
│   └── Weather.kt
├── repository/
│   └── PushSubscriptionRepository.kt
├── scheduler/
│   ├── CacheRefreshScheduler.kt
│   └── PushScheduler.kt          # 정시 푸시 발송
└── service/
    ├── AirKoreaService.kt
    ├── WeatherService.kt
    ├── FcmService.kt             # FCM 연동
    └── PushService.kt            # 푸시 비즈니스 로직
```

## 푸시 알림 동작 방식

1. 사용자가 앱에서 알림 시간 설정 (예: 07:00)
2. 서버가 매 정시마다 스케줄러 실행
3. 해당 시간에 알림 받을 구독자 조회
4. 각 구독자 위치의 날씨/대기질 조회
5. FCM으로 푸시 발송

## TODO

- [ ] AWS 배포
- [ ] GitHub Actions CI/CD
- [ ] 앱 연동
- [ ] 테스트 코드
