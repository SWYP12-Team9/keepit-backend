# 🌟 Keepit : 나의 링크 보관소 🌟
<img width="1276" height="710" alt="keepit-소개" src="https://github.com/user-attachments/assets/205f38ab-b1a3-47d0-abbb-cd8351f892cf" />


## Tech Stack

### Backend
- Java 25
- Spring Boot 3.5.9
- Spring Data JPA (Hibernate)
- Spring Security + OAuth2
- Spring AI (OpenAI 연동)
- QueryDSL 5.1.0

### Scraping Service (Python)
- FastAPI + Uvicorn
- trafilatura (웹 본문 추출)
- BeautifulSoup4 + lxml (HTML 파싱)
- pytubefix + youtube-transcript-api (YouTube 콘텐츠)
- Apify Client (Instagram 스크래핑)

### Database
- MySQL 8.0

### Infrastructure
- NCP Server (애플리케이션 서버)
- NCP Cloud DB for MySQL 8.0 (DB 서버)
- NCP Object Storage (이미지 스토리지)
- Elasticsearch 8.17 (벡터 검색 기반 추천, AI 요약)
- Nginx (리버스 프록시, SSL)
- Docker & Docker Compose

### CI/CD
- GitHub Actions
- Docker Hub

### External Services
- OpenAI API (링크 AI 요약 및 임베딩)

### Documentation
- SpringDoc OpenAPI 3 (Swagger)

### Testing
- JUnit 5 + Mockito

## Architecture
<img width="2816" height="1536" alt="keepit-architecture" src="https://github.com/user-attachments/assets/7a0b559b-44f8-47e7-a10f-4603df17f589" />

```
[GitHub Actions] ──빌드/push──▶ [Docker Hub]
                                      │
                                     pull
                                      │
                                      ▼
[Client]                        [NCP Server]
   │                                  │
   ▼                                  │
[Nginx] ── SSL/TLS (Let's Encrypt) ◀──┘
   │
   ├──▶ [Spring App]  ── NCP Cloud DB for MySQL
   │         │
   │         ├──▶ [Scraper]  (Python FastAPI)
   │         ├──▶ [Elasticsearch]
   │         └──▶ OpenAI API (AI 요약 및 추천)
   │
   └──▶ [Kibana]  (Elasticsearch 관리)
```

## Getting Started

### Prerequisites

- Java 25
- Docker & Docker Compose

### Environment Setup

1. 전체 스택 실행 (Nginx, Elasticsearch, Kibana, Scraper, Spring App)

```bash
docker-compose up -d
```

2. Spring 애플리케이션만 로컬 실행

```bash
./gradlew bootRun
```

또는

```bash
./gradlew build
java -jar build/libs/server-0.0.1-SNAPSHOT.jar
```

## Project Structure

API 레이어와 도메인 레이어를 분리한 패키지 구조를 사용합니다.

```
src/main/java/swyp12/team9/server/
├── global/                    # 전역 설정 및 공통 기능
│   ├── config/               # 설정 클래스 (Security, Web, OAuth2, RestClient)
│   ├── exception/            # GlobalExceptionHandler, ErrorCode
│   ├── annotation/           # @CurrentUserId, @ApiSpec 등 커스텀 어노테이션
│   ├── common/               # 공통 DTO, BaseEntity
│   ├── security/             # JWT 필터, 커스텀 인증 프로바이더
│   ├── filter/               # HTTP 요청/응답 필터
│   └── util/                 # PaginationUtils 등 유틸리티
├── api/                       # API 레이어 (Controller, DTO, Swagger)
│   ├── user/                 # 사용자 API
│   ├── userlink/             # 저장 링크 API
│   ├── reference/            # 레퍼런스 API
│   ├── link/                 # 링크 스크래핑 API
│   ├── recommendation/       # 추천 API
│   ├── stat/                 # 통계 API
│   ├── image/                # 이미지 API
│   ├── jwt/                  # JWT API
│   ├── auth/                 # 인증 API
│   └── terms/                # 약관 API
└── domain/                    # 도메인 레이어 (Entity, Repository, Service)
    ├── user/                 # 사용자 도메인
    ├── userlink/             # 저장 링크 도메인
    ├── reference/            # 레퍼런스 도메인
    ├── link/                 # 링크 도메인
    ├── referenceuserlink/    # 레퍼런스-링크 연결
    ├── jwt/                  # JWT 도메인
    ├── auth/                 # 인증 도메인
    ├── image/                # 이미지 도메인
    ├── recommendation/       # 추천 도메인
    ├── stat/                 # 통계 도메인
    └── terms/                # 약관 도메인
```

각 레이어는 아래 구조를 따릅니다:

```
api/{domain}/
├── {Domain}Api.java         # Swagger 문서화 인터페이스
├── {Domain}Controller.java  # Controller 구현체
└── dto/                     # Request/Response DTO (record)

domain/{domain}/
├── model/                   # JPA 엔티티 및 Enum
├── repository/              # Spring Data JPA Repository (QueryDSL)
├── service/                 # 비즈니스 로직
└── exception/               # 도메인별 커스텀 예외
```

## API Endpoints

**인증**

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/v1/auth/login` | 로그인 |
| POST | `/api/v1/auth/logout` | 로그아웃 |
| GET | `/api/v1/oauth2/authorization/{provider}` | 소셜 로그인 (google/naver/kakao) |
| POST | `/api/v1/jwt/exchange` | 소셜 로그인 후 JWT 발급 |
| POST | `/api/v1/jwt/refresh` | JWT 갱신 |

**사용자**

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/v1/users/profile/complete` | 소셜 로그인 후 프로필 완성 |
| GET | `/api/v1/users/info` | 프로필 조회 |
| PATCH | `/api/v1/users/profile` | 프로필 수정 |
| DELETE | `/api/v1/users/profile/image` | 프로필 이미지 삭제 |
| DELETE | `/api/v1/users/profile/background` | 배경 이미지 삭제 |
| DELETE | `/api/v1/users` | 회원 탈퇴 |
| GET | `/api/v1/users/stats` | 사용자 통계 |

**저장 링크**

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/v1/user-links` | 링크 저장 |
| GET | `/api/v1/user-links` | 링크 목록 (커서 페이징) |
| GET | `/api/v1/user-links/{userLinkId}` | 링크 단건 조회 |
| PATCH | `/api/v1/user-links/{userLinkId}` | 링크 수정 |
| DELETE | `/api/v1/user-links/{userLinkId}` | 링크 삭제 |
| POST | `/api/v1/user-links/{userLinkId}/read` | 읽음 처리 |
| GET | `/api/v1/user-links/search` | 링크 키워드 검색 |

**레퍼런스**

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/v1/references` | 레퍼런스 생성 |
| GET | `/api/v1/references` | 레퍼런스 목록 |
| GET | `/api/v1/references/{referenceId}` | 레퍼런스 단건 조회 |
| PATCH | `/api/v1/references/{referenceId}` | 레퍼런스 수정 |
| DELETE | `/api/v1/references/{referenceId}` | 레퍼런스 삭제 |
| GET | `/api/v1/references/frequent` | 자주 사용하는 레퍼런스 |

**링크 스크래핑**

| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/v1/links/scrape` | URL 스크래핑 |

**추천**

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/v1/recommendations` | AI 기반 링크 추천 |
| GET | `/api/v1/recommendations/categories` | 추천 카테고리 목록 |
| GET | `/api/v1/recommendations/search` | 추천 검색 |

**약관**

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/v1/terms/service` | 서비스 이용약관 |
| GET | `/api/v1/terms/privacy` | 개인정보 처리방침 |

## Key Features

### 링크 저장 및 관리
- Python FastAPI 스크래핑 서버로 제목, 설명, 파비콘 자동 추출
- 스크래핑 정보를 바탕으로 AI 요약 생성
- 공개/비공개 설정, 읽음 처리 (firstOpenedAt, lastOpenedAt, viewCount 트래킹)
- why(저장 이유), memo 작성 가능

### 레퍼런스(폴더)
- 링크를 폴더로 분류
- 공개/비공개 설정
- 기본 폴더는 수정/삭제 불가

### 검색
- 일반 검색
- 추천 검색
- why, memo, 제목, AI 요약 필드 대상

### AI 추천
- OpenAI 임베딩 기반 유사 링크 추천
- Elasticsearch 벡터 스토어 활용

### 사용자 통계
- 저장 패턴 (최근 4주)
- 레퍼런스별 링크 수
- 읽음 상태 분석

### 인증
- Google, Naver, Kakao 소셜 로그인 (OAuth2)
- JWT Access/Refresh 토큰

## API Documentation

애플리케이션 실행 후 Swagger UI에서 API 문서를 확인할 수 있습니다.

```
http://localhost:8080/swagger-ui/index.html
```
