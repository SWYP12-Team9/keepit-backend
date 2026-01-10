# SWYP Server

SWYP Team 9 Backend Server

## Tech Stack

### Backend
- Java 25
- Spring Boot 3.5.9
- Spring Data JPA
- Spring Security
- Spring Validation

### Database
- MySQL 8.0

### Documentation
- SpringDoc OpenAPI 3 (Swagger)

## Getting Started

### Prerequisites

- Java 25
- Docker & Docker Compose

### Environment Setup

1. MySQL 컨테이너 실행

```bash
docker-compose up -d
```

2. 애플리케이션 실행

```bash
./gradlew bootRun
```

또는

```bash
./gradlew build
java -jar build/libs/server-0.0.1-SNAPSHOT.jar
```

## Project Structure

도메인 중심 패키지 구조를 사용합니다.

```
src/main/java/swyp12/team9/server/
├── global/                    # 전역 설정 및 공통 기능
│   ├── config/               # 설정 클래스
│   │   ├── SecurityConfig    # Spring Security 설정 (CORS, 인증/인가)
│   │   └── WebConfig         # Web MVC 설정
│   ├── exception/            # 예외 처리
│   │   ├── GlobalExceptionHandler  # 전역 예외 핸들러
│   │   ├── ErrorCode         # 에러 코드 정의 (Enum)
│   │   └── Custom Exceptions # 커스텀 예외
│   └── common/
│       ├── dto/              # 공통 DTO
│       │   └── ErrorResponse # 에러 응답 DTO
│       └── entity/           # 공통 엔티티
│           └── BaseEntity    # 기본 엔티티 (생성일, 수정일)
└── domain/                   # 도메인별 패키지
    └── {domain-name}/        # 각 도메인 (user 등)
        ├── model/            # 도메인 모델
        │   ├── {Entity}      # JPA 엔티티
        │   └── {Enum}        # 도메인 Enum (Status 등)
        ├── dto/              # Request/Response DTO (Record 사용)
        │   ├── {Domain}CreateRequest
        │   ├── {Domain}UpdateRequest
        │   └── {Domain}Response
        ├── repository/       # Spring Data JPA Repository
        ├── service/          # 비즈니스 로직
        ├── controller/       # REST API Controller
        └── exception/        # 도메인별 커스텀 예외
```
## API Documentation

애플리케이션 실행 후 Swagger UI에서 API 문서를 확인할 수 있습니다.

```
http://localhost:8080/swagger-ui/index.html
```

