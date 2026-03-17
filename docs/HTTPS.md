## 개요

HTTP에서 HTTPS로 전환하는 과정을 기록한다.

---

## 기본 용어

| 용어 | 설명 |
|------|------|
| `nginx` | Engine X. 웹 서버 + 리버스 프록시. 클라이언트 요청을 받아서 뒤에 있는 앱(8080)으로 전달 |
| `certbot` | Certificate + Robot. Let's Encrypt SSL 인증서를 자동으로 발급/갱신해주는 도구 |
| `Let's Encrypt` | 무료 SSL 인증서 발급 기관 (비영리, 2015년 시작) |
| `리버스 프록시` | 앞에서 요청을 받아 뒤의 서버로 전달하는 중계자 |
| `SSL/TLS` | Secure Sockets Layer / Transport Layer Security. 암호화 통신 프로토콜 |

---

## Nginx 역사

```
2004년: Igor Sysoev (러시아)가 개발
문제: Apache가 동시 접속 10,000개 처리 못함 (C10K 문제)
해결: 이벤트 기반, 비동기 아키텍처

Apache: 요청마다 프로세스/스레드 생성 → 무거움
Nginx:  단일 스레드 + 이벤트 루프 → 가벼움

현재: 전 세계 웹서버 점유율 1위 (2024년 기준)
```

---

## 왜 Nginx가 필요한가?

```
기존 구조:
클라이언트 → :8080 → Spring Boot

새 구조:
클라이언트 → :443 (HTTPS) → Nginx → :8080 → Spring Boot
```

**이유:**
- Spring Boot에 직접 SSL 설정하는 것보다 Nginx가 더 간편
- Nginx가 SSL 처리하고, 뒤에서는 평문 HTTP 사용
- Let's Encrypt + Certbot 조합이 Nginx와 잘 맞음

---

## 설정 과정

### 1. Nginx 설치

```bash
# nginx = 웹 서버 (Apache 같은 것)
# -y = 설치 확인에 자동 yes
sudo yum install nginx -y
```

---

### 2. Certbot 설치

```bash
# certbot = Let's Encrypt 인증서 발급 도구
# python3-certbot-nginx = Nginx 전용 플러그인 (자동 설정)
sudo yum install certbot python3-certbot-nginx -y
```

---

### 3. Nginx 설정 파일 생성

```bash
sudo vim /etc/nginx/conf.d/api.todaygonggi.com.conf
```

아래 내용 입력 후 저장 (`ESC` → `:wq`):

```nginx
server {
    listen 80;
    server_name api.todaygonggi.com;

    location / {
        # 들어온 요청을 localhost:8080으로 전달
        proxy_pass http://localhost:8080;
        
        # 원본 요청 정보를 헤더에 담아서 전달
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

**각 항목 설명:**

| 항목 | 설명 |
|------|------|
| `listen 80` | HTTP 포트(80)에서 요청 받음 |
| `server_name` | 이 도메인으로 들어온 요청만 처리 |
| `proxy_pass` | 실제 앱 서버 주소 |
| `proxy_set_header Host` | 원래 요청의 호스트명 전달 |
| `X-Real-IP` | 실제 클라이언트 IP 전달 |
| `X-Forwarded-For` | 프록시 경유 IP 체인 |
| `X-Forwarded-Proto` | 원래 프로토콜 (http/https) |

---

### 4. Nginx 시작

```bash
# 설정 파일 문법 검사
sudo nginx -t

# Nginx 시작
sudo systemctl start nginx

# 부팅 시 자동 시작
sudo systemctl enable nginx
```

---

### 5. SSL 인증서 발급

```bash
# certbot이 자동으로:
# 1. Let's Encrypt에서 인증서 발급
# 2. Nginx 설정 파일에 SSL 설정 추가
# 3. HTTP → HTTPS 리다이렉트 설정

sudo certbot --nginx -d api.todaygonggi.com
```

**진행 중 입력:**
- 이메일 주소: 인증서 만료 알림용
- 약관 동의: Y
- 뉴스레터: N (선택)

---

### 6. 자동 갱신 설정

```bash
# Let's Encrypt 인증서는 90일마다 만료
# certbot-renew.timer가 자동으로 갱신해줌

sudo systemctl start certbot-renew.timer
sudo systemctl enable certbot-renew.timer
```

---

## 확인

```bash
# HTTPS 작동 확인
curl https://api.todaygonggi.com/actuator/health

# 인증서 정보 확인
sudo certbot certificates
```

---

## 결과

| 항목 | 값 |
|------|-----|
| URL | https://api.todaygonggi.com |
| 인증서 위치 | /etc/letsencrypt/live/api.todaygonggi.com/ |
| 인증서 만료 | 90일 (자동 갱신) |

---

## 관련 파일

- Nginx 설정: `/etc/nginx/conf.d/api.todaygonggi.com.conf`
- 인증서: `/etc/letsencrypt/live/api.todaygonggi.com/fullchain.pem`
- 개인키: `/etc/letsencrypt/live/api.todaygonggi.com/privkey.pem`

---

## 문제 해결

```bash
# Nginx 로그 확인
sudo tail -f /var/log/nginx/error.log

# Nginx 재시작
sudo systemctl restart nginx

# 인증서 수동 갱신 테스트
sudo certbot renew --dry-run
```
