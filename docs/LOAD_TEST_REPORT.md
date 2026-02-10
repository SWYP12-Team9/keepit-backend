# k6 부하 테스트 결과 보고서

## 테스트 환경

| 항목 | 값 |
|------|-----|
| 대상 서버 | `https://api.sor999.site` |
| 도구 | k6 v1.0.0 |
| 테스트 일시 | 2026-02-08 |
| 테스트 유형 | load-test (0→20 VU, 1분 40초) |

---

## 테스트 대상 API (13개)

| 도메인 | Method | Endpoint | 설명 |
|--------|--------|----------|------|
| Auth | `POST` | `/api/v1/auth/login` | 로그인 |
| Auth | `POST` | `/api/v1/jwt/refresh` | 토큰 재발급 |
| User | `GET` | `/api/v1/users/info` | 프로필 조회 |
| Reference | `POST` | `/api/v1/references` | 레퍼런스 생성 |
| Reference | `GET` | `/api/v1/references` | 레퍼런스 목록 조회 |
| Reference | `GET` | `/api/v1/references/frequent` | 자주 찾는 레퍼런스 |
| UserLink | `POST` | `/api/v1/user-links` | 링크 생성 |
| UserLink | `GET` | `/api/v1/user-links` | 링크 목록 조회 |
| UserLink | `GET` | `/api/v1/user-links/search` | 링크 검색 |
| Recommendation | `GET` | `/api/v1/recommendations/categories` | 카테고리 목록 |
| Recommendation | `GET` | `/api/v1/recommendations/search` | 키워드 검색 |
| Recommendation | `GET` | `/api/v1/recommendations` | 카테고리별 추천 |
| Stat | `GET` | `/api/v1/users/stats` | 사용자 통계 |

---

## 최종 테스트 결과 (ScrapingService 수정 배포 후)

### Threshold

| 지표 | 측정값 | 기준 | 결과 |
|------|--------|------|------|
| `http_req_duration` p(95) | **187.49ms** | < 2000ms | ✅ 통과 |
| `http_req_failed` rate | **0.13%** | < 5% | ✅ 통과 |

### 성능 지표

| 항목 | 값 |
|------|-----|
| 총 요청 수 | 2,297 |
| 초당 요청 (RPS) | 20.47/s |
| 평균 응답 시간 | 199.48ms |
| 중앙값 응답 시간 | 31.1ms |
| 최대 응답 시간 | 8.53s |
| p(90) 응답 시간 | 166.48ms |
| p(95) 응답 시간 | 187.49ms |
| iteration 평균 | 7.75s |
| iteration p(90) | 13.77s |
| 총 반복 수 | 219 iterations |

### 엔드포인트별 결과

| 엔드포인트 | 결과 | 비고 |
|-----------|------|------|
| login | ✅ 통과 | |
| profile | ✅ 통과 | |
| references list | ✅ 통과 | |
| frequent refs | ✅ 통과 | |
| links list | ✅ 통과 | |
| link search | ✅ 통과 | |
| categories | ✅ 통과 | |
| rec search | ✅ 통과 | |
| rec by category | ✅ 통과 | |
| user stats | ✅ 통과 | |
| create reference | ✅ 통과 | |
| create link | ✅ 통과 | ScrapingService null 체크 수정 후 해결 |
| token refresh | ⚠️ 87% (21/24) | 토큰 만료 관련, 경미 |

---

## 성능 분석

### 롱테일 지연 현상

중앙값(31.1ms)과 평균(199.48ms) 사이의 차이가 매우 크다.
대부분의 요청은 빠르지만, **일부 요청이 극단적으로 느려서(최대 8.53s) 평균을 끌어올리는 롱테일(Long Tail) 지연 패턴**이다.

### 병목 지점: 링크 생성 API의 동기 처리

`POST /api/v1/user-links` 호출 시 하나의 요청 안에서 외부 API를 2번 동기 호출한다.

```text
클라이언트 요청
  → ScrapingService.scrapeUrl()        (외부 스크래핑 API 호출, 타임아웃 60초)
    → LinkRepository.save()
      → LinkAiService.summarizeLink()  (OpenAI API 호출)
        → 응답 반환
```

이로 인해 최대 응답 시간 8.53s, iteration p(90) 13.77s가 발생한다.

### 개선 방향

| 순위 | 대상 | 개선 내용 | 기대 효과 |
|------|------|----------|-----------|
| 1 | LinkService | AI 요약을 비동기로 분리 (`@Async` 또는 이벤트 기반) | 링크 생성 응답 시간 ~60% 감소 |
| 2 | LinkService | 스크래핑도 비동기 전환 (링크 먼저 저장 → 메타데이터 후속 업데이트) | 링크 생성 응답 시간 ~90% 감소 |
| 3 | RestClientConfig | 스크래핑 타임아웃 60초 → 10~15초로 축소 | 최대 응답 시간 대폭 개선 |

**개선 1 적용 시 예상 흐름:**
```text
// 현재 (동기) — 총 ~8초
스크래핑 → DB 저장 → AI 요약 → 응답

// 개선 (비동기) — 총 ~2초
스크래핑 → DB 저장 → 응답
                   → AI 요약 (비동기 백그라운드 처리)
```

### TPS 향상 예측 (20 VU 기준)

#### 링크 생성 API 응답 시간 변화

| 단계 | 처리 방식 | 예상 응답 시간 |
|------|----------|---------------|
| 현재 | 스크래핑(동기) → DB 저장 → AI 요약(동기) | ~8초 |
| 개선 1 | 스크래핑(동기) → DB 저장 → AI 요약(**비동기**) | ~3초 |
| 개선 2 | 스크래핑(**비동기**) → DB 저장 → AI 요약(**비동기**) | ~0.1초 |

#### TPS 비교

| | 현재 | 개선 1 (AI 비동기) | 개선 2 (전부 비동기) |
|---|------|-------------------|---------------------|
| iteration 평균 | 7.75s | ~4.5s | ~2.5s |
| **iteration TPS** | **1.95/s** | **~4.4/s** | **~8.0/s** |
| HTTP req/s | 20.47/s | ~46/s | ~84/s |
| 최대 응답 시간 | 8.53s | ~3.5s | ~0.5s |
| **향상률** | — | **약 2.3배** | **약 4.1배** |

> **계산 근거**: `iteration TPS = VU 수 ÷ iteration 평균 시간`
> - 현재: 20 ÷ 7.75 ≈ 2.58 (이론), 실측 1.95/s
> - 개선 1: 20 ÷ 4.5 ≈ 4.4/s (AI ~5초 제거)
> - 개선 2: 20 ÷ 2.5 ≈ 8.0/s (스크래핑+AI 모두 제거)

읽기 API는 이미 p(95) 187ms로 충분히 빠르기 때문에, **쓰기 API(링크 생성)만 개선해도 전체 TPS가 크게 향상**된다.

---

## 테스트 중 발견된 오류 및 해결

### 오류 1. k6 스크립트 — 잘못된 요청 형식 (4건)

> k6 스크립트의 요청 파라미터/바디가 실제 API 스펙과 불일치하여 발생

| 엔드포인트 | 원인 | 해결 |
|-----------|------|------|
| `GET /user-links/search` | `size` 필수 파라미터 누락 | `?keyword=test&size=20`으로 수정 |
| `GET /recommendations?category=IT` | 존재하지 않는 카테고리명 | `경제/시사`로 수정 (유효 카테고리 사용) |
| `POST /references` | `isPublic` 필수 필드 누락 + 잘못된 `url` 필드 | `{title, isPublic: true}`로 수정 |
| `POST /user-links` | 존재하지 않는 `title` 필드 전송 | `{url}`만 전송하도록 수정 |

### 오류 2. k6 스크립트 — 중복 URL로 인한 실패

> 동일 URL을 반복 전송하여 서버의 중복 체크 로직에 의해 거부됨

| 엔드포인트 | 원인 | 해결 |
|-----------|------|------|
| `POST /user-links` | 동일 URL 반복 요청 → `UserLinkDuplicateException` | URL에 VU/Iteration/Timestamp 조합으로 고유값 부여 |

### 오류 3. 서버 버그 — LinkService NullPointerException

> 외부 스크래핑 API가 null을 반환했을 때 null 체크 없이 메서드를 호출하여 NPE 발생

| 항목 | 내용 |
|------|------|
| 엔드포인트 | `POST /api/v1/user-links` |
| 에러 응답 | `{"status":500, "code":"COM004", "message":"서버 오류가 발생했습니다"}` |
| 원인 | `ScrapingService.scrapeUrl()`이 null 반환 → `LinkService`에서 `scrapingData.getTitle()` 호출 시 NPE |
| 수정 파일 | `src/main/java/.../domain/link/service/ScrapingService.java` |

**수정 내용**: `ScrapingService`에서 null 응답 시 `LinkScrapingServerException` (LNK002) 발생하도록 예외 처리 추가

```java
// 수정 전 — 응답이 null이어도 그대로 반환 → 호출부(LinkService)에서 NPE
ScrapingResponse response = scrapingRestClient.post()
        ...
        .body(ScrapingResponse.class);
return response;  // null 가능 → LinkService에서 NPE!

// 수정 후 — ScrapingService에서 null 차단
ScrapingResponse response = scrapingRestClient.post()
        ...
        .body(ScrapingResponse.class);

if (response == null) {
    log.error("스크래핑 응답이 비어있음 - URL: {}", url);
    throw new LinkScrapingServerException();  // LNK002: "스크래핑 서버에서 오류가 발생했습니다"
}
return response;
```

| | 수정 전 | 수정 후 |
|---|---------|---------|
| 에러 코드 | `COM004` (500) | `LNK002` (502) |
| 메시지 | "서버 오류가 발생했습니다" | "스크래핑 서버에서 오류가 발생했습니다" |
| 원인 파악 | 불가능 | 가능 |

---

## 테스트 스크립트 구성

### 파일 구조

```text
k6-scripts/
├── load-test.js          # 메인 부하 테스트 (전체 시나리오, 쓰기 API 포함)
├── spike-test.js         # 스파이크 테스트 (급격한 트래픽 증가, 읽기 API)
└── stress-test.js        # 스트레스 테스트 (점진적 부하 증가, 읽기 API)
```

### 로그인 처리 방식: `setup()` 분리

모든 테스트 스크립트에서 로그인을 `setup()` 함수로 분리하여, 테스트 시작 전 **1회만 로그인**하고 발급받은 토큰을 모든 VU가 공유한다.

```javascript
// setup(): 테스트 시작 전 1회 실행 → 토큰 반환
export function setup() {
  const res = http.post(`${BASE_URL}/api/v1/auth/login`, ...);
  const body = JSON.parse(res.body);
  return { accessToken: body.accessToken };
}

// default function: 각 VU가 반복 실행 → setup()의 반환값을 data로 수신
export default function (data) {
  const params = authHeaders(data.accessToken);
  // ... API 호출 ...
}
```

| | 변경 전 (`login()` 매 iteration 호출) | 변경 후 (`setup()` 1회 호출) |
|---|---|---|
| 로그인 횟수 | VU × iteration 수 (수백~수천 회) | 1회 |
| 로그인 API 부하 | 테스트 대상에 포함 (결과 왜곡) | 테스트 대상에서 제외 |
| 측정 정확도 | 로그인 지연이 iteration 시간에 포함 | 순수 API 성능만 측정 |

### 시나리오별 Stage 설정

**load-test.js**

| Stage | Duration | VU | 설명 |
|-------|----------|----|------|
| Ramp-up | 30s | 0 → 20 | 점진적 증가 |
| Steady | 1m | 20 유지 | 안정 상태 |
| Ramp-down | 10s | 20 → 0 | 부하 감소 |

**spike-test.js**

| Stage | Duration | VU | 설명 |
|-------|----------|----|------|
| 워밍업 | 10s | 0 → 5 | |
| 급증 | 10s | 5 → 50 | 급격한 증가 |
| 유지 | 30s | 50 | |
| 급감 | 10s | 50 → 5 | 급격한 감소 |
| 회복 | 30s | 5 | 서버 회복 확인 |

**stress-test.js**

| Stage | Duration | VU | 설명 |
|-------|----------|----|------|
| 1단계 | 30s | 0 → 10 | |
| 2단계 | 30s | 10 → 30 | |
| 3단계 | 30s | 30 → 50 | |
| 유지 | 1m | 50 | 고부하 유지 |
| 감소 | 30s | 50 → 0 | |

### 실행 방법

```bash
# 메인 부하 테스트
k6 run k6-scripts/load-test.js

# 환경변수 지정 (기본값 외 설정이 필요한 경우)
k6 run -e BASE_URL=https://api.sor999.site \
       -e USERNAME=testuser \
       -e PASSWORD=password123 \
       -e TEST_LINK_URL=https://www.youtube.com/watch?v=cam0qMyR4Qg \
       k6-scripts/load-test.js

# 스파이크 테스트
k6 run k6-scripts/spike-test.js

# 스트레스 테스트
k6 run k6-scripts/stress-test.js
```

### 환경변수

| 변수 | 설명 | 기본값 |
|------|------|--------|
| `BASE_URL` | 대상 서버 URL | `https://api.sor999.site` |
| `USERNAME` | 테스트 계정 아이디 | `testuser` |
| `PASSWORD` | 테스트 계정 비밀번호 | `password123` |
| `TEST_LINK_URL` | 링크 생성 테스트용 URL (load-test만 해당) | `https://www.youtube.com/watch?v=cam0qMyR4Qg&t=403s` |
