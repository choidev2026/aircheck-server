# AirCheck Server 🌤️

에어체크 앱의 백엔드 서버

## 기술 스택

- **Spring Boot 3.4.4** + Kotlin
- **Caffeine Cache** (인메모리 캐시)
- **OkHttp** (HTTP 클라이언트)

## API 엔드포인트

### 통합 API (날씨 + 대기질)
```
GET /api/v1/weather?lat={위도}&lng={경도}
```

**응답:**
```json
{
  "weather": {
    "current": {
      "temperature": 15.2,
      "feelsLike": 13.8,
      "weatherCondition": "CLEAR",
      ...
    },
    "hourlyForecast": [...]
  },
  "airQuality": {
    "stationName": "종로구",
    "pm10": 45,
    "pm25": 23,
    "worstGrade": "MODERATE",
    ...
  }
}
```

### 날씨만
```
GET /api/v1/weather/forecast?lat={위도}&lng={경도}
```

### 대기질만
```
GET /api/v1/weather/air?lat={위도}&lng={경도}
```

### 헬스체크
```
GET /actuator/health
```

## 캐싱 전략

| 캐시 | TTL | 설명 |
|------|-----|------|
| `airquality` | 10분 | 대기질 데이터 |
| `weather` | 10분 | 날씨 데이터 |
| `sido` | 10분 | 시도명 (Nominatim) |

## 환경 변수

| 변수 | 설명 | 필수 |
|------|------|------|
| `AIRKOREA_API_KEY` | 에어코리아 API 키 | ✅ |
| `SERVER_PORT` | 서버 포트 (기본: 8080) | |

## 실행

### 로컬 실행
```bash
export AIRKOREA_API_KEY=your-api-key
./gradlew bootRun
```

### 빌드
```bash
./gradlew build
java -jar build/libs/aircheck-server-0.0.1-SNAPSHOT.jar
```

### Docker
```bash
docker build -t aircheck-server .
docker run -p 8080:8080 -e AIRKOREA_API_KEY=xxx aircheck-server
```

## 프로젝트 구조

```
src/main/kotlin/com/seriouschoi/aircheck/
├── AircheckServerApplication.kt    # 메인
├── config/
│   └── CacheConfig.kt              # 캐시 설정
├── controller/
│   └── WeatherController.kt        # REST API
├── model/
│   ├── AirQuality.kt               # 대기질 모델
│   └── Weather.kt                  # 날씨 모델
├── scheduler/
│   └── CacheRefreshScheduler.kt    # 스케줄러
└── service/
    ├── AirKoreaService.kt          # 에어코리아 API
    └── WeatherService.kt           # Open-Meteo API
```

## 다음 단계 (TODO)

- [ ] Docker 이미지 빌드
- [ ] 배포 (Railway / Fly.io / AWS)
- [ ] GitHub Actions CI/CD
- [ ] Redis 캐시 (선택)
- [ ] 푸시 알림 (FCM)
