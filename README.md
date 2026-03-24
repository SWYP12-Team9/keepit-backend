  # 🔗 흩어진 링크를 모아 한눈에, AI 링크 관리 서비스 Keepit 
  
  <img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/75acd770-da62-475f-96b3-a596a008bb1c" />
  
  <img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/6a1d215e-22f2-450d-bf7a-f98ee9f9e9e4" />
  
  <img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/70b428bd-a0aa-4771-b768-1f742ed74857" />
  
  ## 🛠 Tech Stack
  
  ### ☕ Backend
  #### 1. Core
  - Java 25
  - Spring Boot 3.5.9
  - Spring Data JPA (Hibernate)
  - Spring Security + OAuth2
  - Spring AI (OpenAI 연동)
  - QueryDSL 5.1.0
  
  #### 2. [Scraping](https://github.com/SWYP12-Team9/keepit-ingestion)
  - Python
  - FastAPI
  - 라이브러리 및 외부 API
    - trafilatura (웹 본문 추출)
    - Playwright (SPA 사이트 추출)
    - BeautifulSoup4 + lxml (HTML 파싱)
    - pytubefix + youtube-transcript-api (YouTube 콘텐츠)
    - Apify Client (Instagram 콘텐츠)
  
  ### 💾 Database
  - MySQL 8.0
  
  ### ☁️ Infrastructure
  - GCP Compute Engine (Nginx, Spring Boot, Elasticsearch, Kibana, Redis)
  - GCP Cloud Run (FastAPI 스크래핑 서버)
  - GCP Cloud SQL for MySQL 8.0 (DB 서버)
  - GCP Cloud Storage (이미지 스토리지)
  - GCP Artifact Registry (Docker 이미지 저장소)
  - Elasticsearch 8.17 (벡터 검색 기반 추천, AI 요약)
  - Redis (캐시 및 이벤트 스트림)
  - Nginx (리버스 프록시, SSL)
  - Docker & Docker Compose
  
  ### 🔄 CI/CD
  - GitHub Actions
  - GCP Artifact Registry
  
  ### 🤖 External Services
  - OpenAI API (링크 AI 요약 및 임베딩)
  
  ### 📝 Documentation
  - SpringDoc OpenAPI 3 (Swagger)
  
  ### 🧪 Testing
  - JUnit 5 + Mockito
  
  ## 🏗 Architecture
  <img width="1898" height="1204" alt="keepit-gcp architecture" src="https://github.com/user-attachments/assets/974766af-0586-4738-948d-9a32e2d70406" />
  
  
  ## 🚀 Getting Started
  
  ### ✅ Prerequisites
  
  - Java 25
  - Docker & Docker Compose
  
  ### ⚙️ Environment Setup
  
  1. 전체 스택 실행 (Mysql, Redis, Elasticsearch, Kibana)
  
  ```bash
  docker-compose up -d
  ```
  
  2. 스크래핑 서버 실행 (별도 레포지토리)
  
  ```bash
  git clone https://github.com/SWYP12-Team9/keepit-ingestion.git
  cd keepit-ingestion
  docker-compose up -d
  ```
  
  3. Spring 애플리케이션 로컬 실행
  
  ```bash
  ./gradlew bootRun
  ```
  
  또는
  
  ```bash
  ./gradlew build
  java -jar build/libs/server-0.0.1-SNAPSHOT.jar
  ```
  
  ## 📁 Project Structure
  
  도메인 중심의 패키지 구조를 사용합니다.
  
  ```
  src/main/java/swyp12/team9/server/
  ├── global/                        # 전역 설정 및 공통 기능
  │   ├── config/                   # 설정 클래스 (Security, Web, OAuth2, RestClient, Redis)
  │   ├── exception/                # GlobalExceptionHandler, ErrorCode
  │   ├── annotation/               # @CurrentUserId, @ApiSpec 등 커스텀 어노테이션
  │   ├── common/                   # 공통 DTO, BaseEntity
  │   ├── security/                 # JWT 필터, 커스텀 인증 프로바이더
  │   ├── filter/                   # HTTP 요청/응답 필터
  │   ├── handler/                  # 글로벌 이벤트 핸들러
  │   ├── infrastructure/           # 외부 인프라 연동 (Cloud Storage 등)
  │   ├── interceptor/              # HTTP 인터셉터
  │   ├── converter/                # 타입 컨버터
  │   ├── resolver/                 # 아규먼트 리졸버
  │   └── util/                     # PaginationUtils 등 유틸리티
  └── domain/                        # 도메인 레이어 (각 도메인이 Controller ~ Repository 수직 소유)
      ├── auth/                     # 인증 (OAuth2, 로그인/로그아웃)
      │   └── jwt/                  # JWT 토큰 관리
      ├── user/                     # 사용자 (프로필, 회원가입, 탈퇴)
      ├── userlink/                 # 저장 링크 (스크래핑, AI 요약, 읽음 처리)
      ├── link/                     # 링크 원본 (스크래핑 데이터, Redis Stream 처리)
      ├── reference/                # 레퍼런스 폴더
      │   └── relation/             # 레퍼런스-링크 연결 관계
      ├── recommendation/           # AI 추천 (Elasticsearch 벡터 검색)
      ├── chatbot/                  # AI 챗봇
      ├── sse/                      # SSE 실시간 알림
      ├── stat/                     # 사용자 통계
      ├── image/                    # 이미지 (Cloud Storage)
      └── terms/                    # 약관
  ```
  
  각 도메인은 아래 구조를 따릅니다:
  
  ```
  domain/{domain}/
  ├── controller/              # Controller + Swagger API 인터페이스
  ├── dto/                     # Request/Response DTO (record)
  ├── service/                 # 비즈니스 로직
  ├── model/                   # JPA 엔티티 및 Enum
  ├── repository/              # Spring Data JPA Repository (QueryDSL)
  ├── event/                   # 도메인 이벤트 (Redis Stream 등)
  ├── infrastructure/          # 외부 서비스 연동 (OpenAI, Scraper 등)
  └── exception/               # 도메인별 커스텀 예외
  ```
  
  ## 📡 API Endpoints
  
  **인증**
  
  | Method | Path | 설명 |
  |--------|------|------|
  | POST | `/api/v1/auth/login` | 로그인 |
  | POST | `/api/v1/auth/logout` | 로그아웃 |
  | GET | `/api/v1/oauth2/authorization/{provider}` | 소셜 로그인 (google/naver/kakao) |
  | POST | `/api/v1/jwt/exchange` | Refresh 토큰을 헤더 기반 JWT로 교환 |
  | POST | `/api/v1/jwt/refresh` | Access/Refresh 토큰 재발급 |
  
  **사용자**
  
  | Method | Path | 설명 |
  |--------|------|------|
  | POST | `/api/v1/users/signup` | 회원가입 |
  | POST | `/api/v1/users/profile/complete` | 소셜 로그인 후 프로필 완성 |
  | GET | `/api/v1/users/info` | 내 프로필 조회 |
  | PATCH | `/api/v1/users/profile` | 프로필 수정 |
  | DELETE | `/api/v1/users/profile/image` | 프로필 이미지 삭제 |
  | DELETE | `/api/v1/users/profile/background` | 배경 이미지 삭제 |
  | DELETE | `/api/v1/users` | 회원 탈퇴 |
  | GET | `/api/v1/users/stats` | 사용자 통계 조회 |
  
  **저장 링크**
  
  | Method | Path | 설명 |
  |--------|------|------|
  | POST | `/api/v1/user-links` | 링크 저장 |
  | GET | `/api/v1/user-links` | 링크 목록 (커서 페이징) |
  | GET | `/api/v1/user-links/{userLinkId}` | 링크 단건 조회 및 읽음 처리 |
  | GET | `/api/v1/user-links/{userLinkId}/preview` | 링크 미리보기 조회 |
  | PATCH | `/api/v1/user-links/{userLinkId}` | 링크 수정 |
  | DELETE | `/api/v1/user-links/{userLinkId}` | 링크 삭제 |
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
  
  **추천 / 탐색**
  
  | Method | Path | 설명 |
  |--------|------|------|
  | GET | `/api/v1/recommendations` | 카테고리별 추천 콘텐츠 조회 |
  | GET | `/api/v1/recommendations/categories` | 추천 카테고리 목록 |
  | GET | `/api/v1/recommendations/search` | 추천 키워드 검색 |
  | GET | `/api/v1/recommendations/popular` | 인기글 조회 |
  | POST | `/api/v1/recommendations/links/{linkId}/view` | 공개 링크 조회수 증가 |
  
  **챗봇**
  
  | Method | Path | 설명 |
  |--------|------|------|
  | POST | `/api/v1/chatbots/message` | AI 챗봇 질의 |
  
  **실시간**
  
  | Method | Path | 설명 |
  |--------|------|------|
  | GET | `/api/v1/sse/subscribe` | SSE 이벤트 구독 |
  
  **관리자**
  
  | Method | Path | 설명 |
  |--------|------|------|
  | POST | `/api/v1/links/scrape` | URL 스크래핑 |
  | POST | `/api/v1/links/summary` | URL 스크래핑 + AI 요약 미리보기 |
  | POST | `/api/v1/recommendations/index` | 전체 공개 링크 색인 |
  | POST | `/api/v1/recommendations/index/link` | 단일 링크 색인 |
  
  **약관**
  
  | Method | Path | 설명 |
  |--------|------|------|
  | GET | `/api/v1/terms/service` | 서비스 이용약관 |
  | GET | `/api/v1/terms/privacy` | 개인정보 처리방침 |
  
## ✨ Key Features

### 🔗 링크 관리
- 링크 저장
  - Cloud Run FastAPI 스크래핑 서버로 제목, 설명, 파비콘 자동 추출
  - Redis Stream 기반 비동기 링크 처리 및 SSE 실시간 알림
  - 스크래핑 정보를 바탕으로 AI 요약 생성
- 링크 조회
  - AI 요약 및 링크 미리보기 조회
  - 단건 조회 시 읽음 처리, 조회수 추적
  - 공개/비공개 설정
  - 저장 이유(why), 메모 작성 가능

### 📂 레퍼런스(폴더)
- 링크를 폴더로 분류
- 공개/비공개 설정
- 기본 폴더는 수정/삭제 불가

### 🔍 검색
- 일반 검색
- 추천 키워드 검색
- why, memo, 제목, AI 요약 필드 대상

### 💡 추천
- 카테고리별 추천
  - OpenAI 임베딩 기반 유사 링크 추천, Elasticsearch 벡터 스토어 활용
- 인기글 조회
  - 공개 링크 조회수 기반 인기 콘텐츠 집계

### 🤖 AI 챗봇
- 사용자가 저장한 링크를 대상으로 RAG 기반 질의응답 제공
- 관련 링크를 근거로 자연어 답변과 참고 링크 목록 반환

### 🔐 인증
- 자체 로그인 및 로그아웃
- Google, Naver, Kakao 소셜 로그인 (OAuth2)
- JWT Access/Refresh 토큰

### 👤 사용자
- 소셜 로그인 지원
- 최초 소셜 로그인 후 프로필 완성 플로우
- 프로필 정보, 프로필 이미지, 배경 이미지 수정 및 삭제

### 📊 사용자 통계
- 저장 패턴 (최근 4주)
- 레퍼런스별 링크 수
- 읽음 상태 분석

### 🛠️ 관리자 기능
- URL 스크래핑 결과 확인
- 스크래핑 결과 기반 AI 요약 미리보기
- 추천용 공개 링크 전체/단건 색인 관리


## 📖 API Documentation

애플리케이션 실행 후 Swagger UI에서 API 문서를 확인할 수 있습니다.

```
http://localhost:8080/swagger-ui/index.html
```
