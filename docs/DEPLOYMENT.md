## 서버 정보

| 항목 | 값 |
|------|-----|
| IP | 13.125.124.253 |
| 인스턴스 | aircheck-prod |
| OS | Amazon Linux 2023 |
| 스펙 | $3.50/월 (512MB RAM, 1 vCPU) |
| 리전 | ap-northeast-2 (서울) |

---

## 기본 명령어 설명

| 명령어 | 설명 |
|--------|------|
| `sudo` | 관리자(root) 권한으로 실행. 시스템 설정 변경 시 필요 |
| `yum` | Yellowdog Updater Modified. Amazon Linux의 패키지 관리자. 소프트웨어 설치/삭제/업데이트 (Ubuntu는 apt, macOS는 brew) |
| `systemctl` | systemd 서비스 관리 명령어 (start/stop/restart/status/enable) |
| `chmod` | 파일 권한 변경. 600 = 소유자만 읽기/쓰기 가능 |
| `chown` | 파일 소유자 변경 |
| `ssh` | Secure Shell. 원격 서버에 암호화된 접속 |
| `scp` | Secure Copy. SSH를 통한 파일 복사 (로컬 ↔ 서버) |
| `curl` | Client URL. URL로 HTTP 요청 보내기 (API 테스트용) |
| `-y` 옵션 | "설치할까요? [y/N]" 질문에 자동으로 yes |

---

## 배포 과정

### 1. Lightsail 인스턴스 생성 (AWS 콘솔)

#### 왜?
서버가 있어야 앱을 돌릴 수 있다. Lightsail은 AWS의 간단한 VPS 서비스로, EC2보다 설정이 쉽고 가격이 고정되어 있다.

#### 방법
```
AWS Console → Lightsail
- Platform: Linux/Unix
- Blueprint: OS Only → Amazon Linux 2023
- Plan: $3.50 (Dual-stack)
- Name: aircheck-prod
```

---

### 2. 방화벽 설정 (AWS 콘솔)

#### 왜?
기본적으로 서버의 모든 포트는 막혀있다. 필요한 포트만 열어야 외부에서 접속 가능하다.

| 포트 | 용도 |
|------|------|
| 22 | SSH 접속 (서버 관리용) |
| 80 | HTTP (나중에 웹서버용) |
| 443 | HTTPS (SSL 암호화 통신) |
| 8080 | Spring Boot 앱 포트 |

#### 방법
Networking → IPv4 Firewall → Add rule:
- SSH (22) - 기본으로 열려있음
- HTTP (80) - 기본으로 열려있음
- HTTPS (443) - **직접 추가**
- Custom TCP 8080 - **직접 추가**

---

### 3. SSH 키 설정 (로컬)

#### 왜?
서버에 접속하려면 비밀번호 대신 SSH 키를 사용한다. 더 안전하고, Lightsail은 기본적으로 키 방식만 허용한다.

#### 방법
```bash
# Lightsail 콘솔에서 키 다운로드 후 저장
~/.ssh/lightsail-aircheck.pem

# 키 파일 권한 설정 (본인만 읽기 가능)
# 이거 안 하면 "too open" 에러 발생
chmod 600 ~/.ssh/lightsail-aircheck.pem

# 서버 접속
# -i = identity file (키 파일 지정)
# ec2-user = Amazon Linux 기본 유저명
ssh -i ~/.ssh/lightsail-aircheck.pem ec2-user@<서버IP>
```

---

## ⚠️ 4번부터는 서버에서 실행 (SSH 접속 후)

---

### 4. Java 17 설치

#### 왜?
Spring Boot 앱은 Java로 만들어졌다. 서버에 Java가 없으면 앱을 실행할 수 없다. Amazon Corretto는 AWS가 관리하는 무료 OpenJDK 배포판이다.

#### 방법
```bash
# yum = 패키지 관리자 (Play Store 같은 것)
# -y = 설치 확인 질문에 자동 yes
sudo yum install java-17-amazon-corretto -y

# 설치 확인
java -version
```

---

### 5. MariaDB 설치 및 설정

#### 왜?
앱에서 데이터를 저장할 데이터베이스가 필요하다. MariaDB는 MySQL과 호환되는 무료 DB이다.

#### 방법
```bash
# MariaDB 서버 설치
sudo yum install mariadb105-server -y

# DB 서버 시작
# systemctl = 서비스 관리 명령어
sudo systemctl start mariadb

# 서버 재부팅 시 자동 시작 등록
# enable = 부팅 시 자동 시작
sudo systemctl enable mariadb
```

#### DB 및 유저 생성

```bash
# mysql -e "쿼리" = 쿼리 한 줄 실행
# aircheck 데이터베이스와 전용 유저 생성
# 앱은 이 유저로 DB에 접속한다
sudo mysql -e "
CREATE DATABASE IF NOT EXISTS aircheck;
CREATE USER IF NOT EXISTS 'aircheck'@'localhost' IDENTIFIED BY '<DB_PASSWORD>';
GRANT ALL PRIVILEGES ON aircheck.* TO 'aircheck'@'localhost';
FLUSH PRIVILEGES;
"
```

#### 보안 설정

```bash
# root 비밀번호 설정 (기본은 비밀번호 없음 = 위험)
sudo mysql -e "ALTER USER 'root'@'localhost' IDENTIFIED BY '<ROOT_PASSWORD>';"

# 익명 사용자 삭제 (보안)
sudo mysql -u root -p'<ROOT_PASSWORD>' -e "DELETE FROM mysql.user WHERE User='';"

# 원격에서 root 접속 차단 (localhost만 허용)
sudo mysql -u root -p'<ROOT_PASSWORD>' -e "DELETE FROM mysql.user WHERE User='root' AND Host NOT IN ('localhost', '127.0.0.1', '::1');"

# 테스트 DB 삭제 (불필요)
sudo mysql -u root -p'<ROOT_PASSWORD>' -e "DROP DATABASE IF EXISTS test;"

# 설정 적용
sudo mysql -u root -p'<ROOT_PASSWORD>' -e "FLUSH PRIVILEGES;"
```

---

### 6. 앱 디렉토리 생성

#### 왜?
JAR 파일을 저장할 위치가 필요하다. `/opt`는 리눅스에서 추가 소프트웨어를 설치하는 관례적인 위치이다.

#### 방법
```bash
# -p = 중간 디렉토리도 함께 생성
sudo mkdir -p /opt/aircheck

# chown = change owner (소유자 변경)
# ec2-user:ec2-user = 유저:그룹
# 이거 안 하면 파일 업로드할 때 권한 에러
sudo chown ec2-user:ec2-user /opt/aircheck
```

---

### 7. Systemd 서비스 등록

#### 왜?
```
systemd 없이 직접 실행하면:
- SSH 접속해서 java -jar app.jar 실행
- 터미널 닫으면 앱도 죽음
- 서버 재부팅하면 수동으로 다시 실행해야 함
- 앱이 크래시하면 그냥 죽어있음

systemd로 서비스 등록하면:
✅ 서버 재부팅 → 자동으로 앱 시작
✅ 앱 크래시 → 10초 후 자동 재시작
✅ 로그 관리 (journalctl로 확인)
✅ 시작/중지/재시작 명령어 통일
```

#### 방법

```bash
# vim = 텍스트 에디터
# /etc/systemd/system/ = 서비스 파일 저장 위치
sudo vim /etc/systemd/system/aircheck.service
```

아래 내용을 붙여넣고 저장 (`ESC` → `:wq` → `Enter`):

```ini
[Unit]
# 서비스 설명 (systemctl status에 표시됨)
Description=Aircheck Server
# 네트워크 준비된 후에 시작 (DB 연결 등 필요)
After=network.target

[Service]
# 단순 실행형 서비스 (실행하면 바로 시작)
Type=simple
# 이 유저 권한으로 실행 (root 아님 = 보안)
User=ec2-user
# 작업 디렉토리 (cd /opt/aircheck 한 것처럼)
WorkingDirectory=/opt/aircheck
# 실제 실행 명령어
# -Xmx256m: JVM 메모리 최대 256MB (서버 스펙에 맞춤)
# --spring.profiles.active=prod: prod 프로필 사용
ExecStart=/usr/bin/java -Xmx256m -jar /opt/aircheck/app.jar --spring.profiles.active=prod
# 종료되면 항상 재시작 (크래시 대응)
Restart=always
# 재시작 전 10초 대기 (바로 재시작하면 같은 에러로 무한루프 가능)
RestartSec=10
# 환경 변수
Environment=DB_PASSWORD=<DB_PASSWORD>

[Install]
# enable하면 부팅 시 자동 시작 (multi-user = 일반 서버 모드)
WantedBy=multi-user.target
```

저장 후:

```bash
# 서비스 파일 수정했으니 systemd에게 다시 읽으라고 알림
sudo systemctl daemon-reload

# 서버 부팅 시 자동 시작하도록 등록
sudo systemctl enable aircheck
```

---

## 8. JAR 빌드 및 업로드 (로컬)

#### 왜?
Spring Boot 앱을 JAR 파일로 패키징해서 서버에 올려야 한다. JAR 파일 하나에 앱 코드 + 모든 라이브러리가 포함된다 (Fat JAR).

#### 방법
```bash
# 로컬에서 빌드
# bootJar = Spring Boot JAR 생성 태스크
./gradlew :app:bootJar

# 서버로 업로드
# scp = Secure Copy (SSH로 파일 복사)
# -i = 키 파일 지정
scp -i ~/.ssh/lightsail-aircheck.pem \
  app/build/libs/app-0.0.1-SNAPSHOT.jar \
  ec2-user@<서버IP>:/opt/aircheck/app.jar
```

---

## 9. 서비스 시작 (서버)

#### 왜?
JAR 업로드했으니 이제 실행할 차례다.

#### 방법
```bash
# 서비스 시작
sudo systemctl start aircheck

# 상태 확인 (active (running) 이면 성공)
sudo systemctl status aircheck
```

---

## 확인 명령어

```bash
# 헬스 체크 (서버가 살아있는지)
# curl = URL로 HTTP 요청
curl http://<서버IP>:8080/actuator/health

# 날씨 API 테스트
curl "http://<서버IP>:8080/api/v1/weather?lat=37.5665&lng=126.9780"

# 실시간 로그 보기
# journalctl = systemd 로그 조회
# -u aircheck = aircheck 서비스만
# -f = follow (실시간으로 계속 따라감, Ctrl+C로 종료)
sudo journalctl -u aircheck -f

# 최근 로그 100줄만 보기
sudo journalctl -u aircheck -n 100

# 서비스 재시작 (코드 업데이트 후)
sudo systemctl restart aircheck

# 서비스 중지
sudo systemctl stop aircheck
```

---

## 환경 변수

```bash
# systemd 서비스 파일의 Environment에 추가하거나
# 별도 .env 파일로 관리
DB_PASSWORD=<DB_PASSWORD>
AIRKOREA_API_KEY=<API_KEY>
FIREBASE_CREDENTIALS_JSON=<JSON>
```

---

## TODO

- [ ] AirKorea API 키 설정
- [ ] 도메인 연결
- [ ] HTTPS (Let's Encrypt)
- [ ] CI/CD 자동 배포


