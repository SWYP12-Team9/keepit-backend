# 추천 카테고리 API 부하테스트 결과

작성일: 2026-04-30

이 문서는 추천 Redis 캐싱 적용 전 기준선 성능을 측정한 결과를 정리한다. 이후 캐싱 적용 브랜치에서 같은 조건으로 cold cache / warm cache를 측정해 전후 비교 자료로 사용한다.

---

## 1. 측정 목적

추천 카테고리 API는 캐싱 적용 전에는 요청마다 다음 작업을 수행한다.

```text
GET /api/v1/recommendations?category={category}&size=10

요청
→ 카테고리명 기반 embedding 생성
→ Elasticsearch vector search
→ 추천 후보 UserLink 조회
→ 응답 생성
```

Redis 캐싱을 적용하면 카테고리별 추천 후보 ID 목록을 재사용할 수 있다. 따라서 캐싱 전 기준선 수치를 먼저 확보해야 캐싱 후 개선 폭을 정량적으로 비교할 수 있다.

---

## 2. 측정 대상

대상 API:

```text
GET /api/v1/recommendations?category={category}&size=10
```

테스트 스크립트:

```text
k6-scripts/recommendation-category-benchmark.js
```

테스트 대상 카테고리:

```text
경제/시사
뷰티/패션
요리/식품
운동/건강
인문/지식
직장/자기개발
홈/리빙
기타
```

각 iteration은 위 8개 카테고리 중 하나를 랜덤으로 선택해 요청한다. 이는 실제 사용자가 탐색 탭에서 여러 카테고리를 조회하는 상황을 단순화한 시나리오다.

---

## 3. 측정 환경

기준 브랜치:

```text
develop
```

캐싱 상태:

```text
추천 Redis 캐싱 적용 전
```

측정 대상 서버:

```text
https://dev.keepit.im
```

인증 여부:

```text
USE_AUTH=false
```

카테고리 추천 조회 API는 익명 접근이 허용되어 있어 로그인 없이 측정했다. 로그인 계정을 사용하지 않았기 때문에 “내가 이미 저장한 링크 제외” 로직은 비로그인 기준으로 동작한다. 이후 캐싱 적용 후 비교도 같은 조건으로 맞춰야 한다.

---

## 4. 실행 조건

k6 옵션:

| 항목 | 값 |
|---|---:|
| VUs | 20 |
| steady duration | 60s |
| ramp-up | 15s |
| ramp-down | 10s |
| size | 10 |
| warmup | 0 |

실제 실행 명령:

```bash
K6_WEB_DASHBOARD=false k6 run \
  --quiet \
  --summary-export /tmp/keepit-develop-category-baseline.json \
  -e BASE_URL=https://dev.keepit.im \
  -e USE_AUTH=false \
  -e VUS=20 \
  -e DURATION=60s \
  -e WARMUP_REQUESTS=0 \
  k6-scripts/recommendation-category-benchmark.js
```

결과 JSON:

```text
/tmp/keepit-develop-category-baseline.json
```

---

## 5. 측정 결과

| 조건 | 요청 수 | 실패율 | RPS | avg | p50 | p90 | p95 | max |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| develop 캐싱 전 | 2,206 | 0.00% | 25.93 | 661ms | 685ms | 845ms | 934ms | 1.44s |

원본 k6 요약:

```text
checks_total: 4,412
checks_succeeded: 100.00%
checks_failed: 0.00%

http_reqs: 2,206
http_req_failed: 0.00%

rec_category_duration_ms:
  avg: 661.30ms
  min: 134ms
  med: 684.91ms
  max: 1.44s
  p90: 844.73ms
  p95: 934.19ms
```

---

## 6. 결과 해석

캐싱 전 develop 기준으로 `VUS=20`, steady `60s` 조건에서 추천 카테고리 API의 p95는 약 `934ms`로 측정됐다.

실패율은 `0.00%`였기 때문에 기능 안정성은 문제없었다. 다만 평균 응답시간이 `661ms`, p95가 `934ms`로 나와 추천 조회가 비교적 무거운 API임을 확인할 수 있다.

캐싱 전 구조에서는 카테고리 추천 요청마다 Elasticsearch vector search가 반복된다. 따라서 요청 수가 늘어나면 Elasticsearch와 embedding 처리 비용이 요청량에 비례해서 증가한다.

이번 기준선은 캐싱 적용 후 warm cache 측정 결과와 비교해야 의미가 있다. 특히 다음 항목을 비교하면 된다.

- p95 응답시간 감소율
- 평균 응답시간 감소율
- 동일 조건에서 처리량 변화
- 실패율 유지 여부
- Elasticsearch 호출 감소 여부

---

## 7. 캐싱 후 비교 계획

캐싱 적용 브랜치에서 같은 스크립트와 같은 조건으로 두 번 측정한다.

### 7-1. 캐싱 후 cold cache

Redis 추천 캐시를 비운 뒤 바로 측정한다.

```bash
K6_WEB_DASHBOARD=false k6 run \
  --quiet \
  --summary-export /tmp/keepit-cache-cold-category.json \
  -e BASE_URL=https://dev.keepit.im \
  -e USE_AUTH=false \
  -e VUS=20 \
  -e DURATION=60s \
  -e WARMUP_REQUESTS=0 \
  k6-scripts/recommendation-category-benchmark.js
```

### 7-2. 캐싱 후 warm cache

8개 카테고리를 워밍업한 뒤 측정한다.

```bash
K6_WEB_DASHBOARD=false k6 run \
  --quiet \
  --summary-export /tmp/keepit-cache-warm-category.json \
  -e BASE_URL=https://dev.keepit.im \
  -e USE_AUTH=false \
  -e VUS=20 \
  -e DURATION=60s \
  -e WARMUP_REQUESTS=1 \
  k6-scripts/recommendation-category-benchmark.js
```

비교 표 템플릿:

| 조건 | 요청 수 | 실패율 | RPS | avg | p50 | p90 | p95 | max | p95 개선율 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| develop 캐싱 전 | 2,206 | 0.00% | 25.93 | 661ms | 685ms | 845ms | 934ms | 1.44s | - |
| 캐싱 후 cold |  |  |  |  |  |  |  |  |  |
| 캐싱 후 warm |  |  |  |  |  |  |  |  |  |

개선율 계산:

```text
p95 개선율 = (캐싱 전 p95 - 캐싱 후 p95) / 캐싱 전 p95 * 100
```

예시:

```text
캐싱 전 p95 = 934ms
캐싱 후 p95 = 300ms

(934 - 300) / 934 * 100 = 67.9%
```

---

## 8. 주의사항

정확한 전후 비교를 위해 아래 조건을 유지해야 한다.

- 같은 서버 환경에서 측정한다.
- 같은 DB/Elasticsearch 데이터셋을 사용한다.
- 같은 k6 스크립트를 사용한다.
- 같은 `VUS`, `DURATION`, `size`, 인증 조건을 사용한다.
- 캐싱 후 cold/warm 측정 전 Redis 상태를 명확히 구분한다.
- 가능하면 서버 외부 트래픽이 적은 시간대에 측정한다.

이번 기준선 측정은 `USE_AUTH=false`로 진행했으므로, 캐싱 후 비교도 우선 동일하게 비로그인 조건으로 맞춘다. 로그인 사용자 기준 성능은 별도 테스트로 분리하는 것이 좋다.

---

## 9. 현재 결론

캐싱 적용 전 develop 기준 추천 카테고리 API는 다음 기준선을 가진다.

```text
p95: 934ms
avg: 661ms
RPS: 25.93
실패율: 0.00%
```

캐싱 적용 후 warm cache에서 p95와 avg가 유의미하게 감소하면 `categoryRecommendationIds` 캐싱이 Elasticsearch vector search 비용을 줄였다고 볼 수 있다.

