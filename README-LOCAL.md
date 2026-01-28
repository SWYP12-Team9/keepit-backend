# 로컬 테스트 환경 설정 가이드

## 1. 로컬 MySQL 실행 (Docker)

```bash
# MySQL 컨테이너 시작
docker-compose up -d

# MySQL 컨테이너 상태 확인
docker ps

# MySQL 로그 확인
docker logs keepit-mysql-local

# MySQL 컨테이너 중지
docker-compose down

# MySQL 데이터까지 모두 삭제 (완전 초기화)
docker-compose down -v
```

## 2. 애플리케이션 실행

### 방법 1: Gradle 명령어
```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

### 방법 2: IntelliJ IDEA
1. `ServerApplication` 클래스의 `main` 메서드 옆 실행 버튼 클릭
2. `Edit Configurations...` 선택
3. `Environment variables` 또는 `VM options`에 추가:
   - Environment: `SPRING_PROFILES_ACTIVE=local`
   - 또는 VM options: `-Dspring.profiles.active=local`

## 3. Swagger UI 접속

애플리케이션 실행 후:
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- API Docs: http://localhost:8080/v3/api-docs

## 4. MySQL 직접 접속 (필요시)

```bash
# Docker 컨테이너 내부 접속
docker exec -it keepit-mysql-local mysql -u localuser -p

# 비밀번호: localpassword
```

또는 MySQL Workbench, DBeaver 등의 클라이언트로 접속:
- Host: localhost
- Port: 3306
- Username: localuser
- Password: localpassword
- Database: swyp_db

## 5. 프로파일 설명

- `local`: 로컬 개인 테스트용 (docker-compose MySQL 사용)
- `dev`: 공통 개발 서버용 (NCP MySQL 사용)
- `prod`: 운영 서버용

## 6. 문제 해결

### Port 3306이 이미 사용 중인 경우
```bash
# 기존 MySQL 프로세스 확인
sudo lsof -i :3306

# 또는 docker-compose.yml에서 포트 변경
ports:
  - "3307:3306"  # 로컬 3307 포트로 변경
  
# 그리고 application-local.yaml에서도 포트 변경
url: jdbc:mysql://localhost:3307/swyp_db?...
```

### Docker가 설치되지 않은 경우
1. Docker Desktop 설치: https://www.docker.com/products/docker-desktop
2. 또는 MySQL 직접 설치: https://dev.mysql.com/downloads/mysql/
