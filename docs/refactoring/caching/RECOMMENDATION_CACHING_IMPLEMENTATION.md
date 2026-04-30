# 추천 Redis 캐싱 구현 정리

브랜치: `feat/#107-recommendation-caching`

이 문서는 추천 API Redis 캐싱의 구상 단계, 최종 설계, 현재 코드 흐름, 캐시 무효화 정책, 검증 방법을 한곳에 정리한다. 기존에 분리되어 있던 캐싱 분석/설계/벤치마크 문서는 현재 코드와 맞지 않는 내용이 섞여 있어 이 파일로 통합했다.

---

## 1. 문제 상황

탐색 탭의 카테고리 추천 API는 요청마다 비용이 큰 작업을 반복했다.

```text
GET /api/v1/recommendations?category=경제/시사

요청
→ 카테고리 문자열 embedding 생성
→ Elasticsearch vector search
→ 현재 사용자가 저장한 linkId DB 조회
→ 추천 UserLink 상세 DB 조회
→ 응답 생성
```

병목은 크게 두 가지였다.

- OpenAI embedding + Elasticsearch vector search는 외부 API/검색 엔진 비용이 크다.
- `findLinkIdsByUserId(userId)`는 추천/검색 요청마다 반복되는 DB 조회다.

카테고리는 고정된 enum 값이므로 동일 카테고리에 대한 추천 후보군은 여러 사용자가 공유할 수 있다. 이 특성을 이용해 Redis 캐싱을 적용했다.

---

## 2. 구상 단계에서 검토한 방향

### 2-1. 최종 응답 DTO 캐싱

처음에는 `RecommendationResponse` 전체를 캐싱하는 방법을 고려할 수 있었다.

```text
category + userId + cursor + size -> PageResponse<RecommendationResponse>
```

하지만 이 방식은 캐시 키 조합이 많고 무효화가 어렵다. 사용자별 저장 링크 제외, 공개/비공개 변경, 링크 삭제, 인덱싱 변경이 모두 최종 응답에 영향을 준다. 그래서 최종 응답은 캐싱하지 않는다.

### 2-2. 카테고리별 추천 후보 ID 캐싱

최종 선택한 방식은 카테고리별 추천 후보 `UserLink ID` 목록만 캐싱하는 것이다.

```text
category -> List<userLinkId>
```

이 방식은 모든 사용자가 같은 카테고리 후보군을 공유할 수 있고, 응답 생성 시 사용자별 제외 로직만 별도로 적용하면 된다.

### 2-3. `topK`를 캐시 키에 넣지 않은 이유

초기 아이디어는 `category:topK` 형태였다.

```text
page 1 -> topK=50  -> 경제 시사:50
page 2 -> topK=100 -> 경제 시사:100
page 3 -> topK=150 -> 경제 시사:150
```

이러면 페이지가 바뀔 때마다 캐시 미스가 발생한다. 현재 구현은 `topK=1000`으로 고정하고 캐시 키는 카테고리만 사용한다. 그래서 모든 페이지가 같은 캐시 엔트리를 공유한다.

### 2-4. ES pre-filtering에서 in-memory filtering으로 변경한 이유

기존 방식은 사용자가 저장한 linkId를 Elasticsearch filter에 넣어 제외했다.

```text
indexType == 'recommendation' && linkId nin [1, 2, 3]
```

이 방식은 사용자마다 ES 쿼리가 달라지므로 카테고리 추천 결과를 공유 캐시로 만들기 어렵다.

현재 방식은 카테고리 후보군을 사용자와 무관하게 캐싱하고, 현재 사용자가 이미 저장한 링크는 Java 메모리에서 제외한다.

```text
categoryRecommendationIds["경제 시사"] -> [101, 205, 333]
userLinkIds[7] -> [3, 8, 205]
결과 -> [101, 333]
```

---

## 3. 최종 캐시 구조

| 캐시 이름 | 키 | 값 | TTL | 목적 |
|---|---|---|---:|---|
| `categoryRecommendationIds` | `processedCategory` | `List<Long>` userLinkIds | 30분 | OpenAI embedding + ES vector search 절약 |
| `userLinkIds` | `userId` | `List<Long>` linkIds | 10분 | 사용자 저장 링크 ID DB 조회 절약 |
| `popularLinks` | `size` 등 기존 key | 인기글 응답 | 5분 | 기존 인기글 캐시 유지 |

중요한 차이:

- `categoryRecommendationIds`는 `UserLink ID`를 저장한다. 추천 응답에는 공개 저장자 정보가 필요하기 때문이다.
- `userLinkIds`는 `Link ID`를 저장한다. 사용자가 이미 같은 원본 링크를 저장했는지 비교하기 위해서다.

설정 위치:

- `RedisConfig.cacheManager()`
- `RecommendationCacheService`
- `ServerApplication`의 `@EnableCaching`

---

## 4. 현재 조회 흐름

### 4-1. 카테고리 추천

대상 API:

```text
GET /api/v1/recommendations?category={category}
```

현재 흐름:

```text
RecommendationController
→ RecommendationService.getRecommendationsByCategory()
→ RecommendationCacheService.getCategoryRecommendationUserLinkIds()
   → cache hit: Redis에서 userLinkId 목록 반환
   → cache miss: ES vector search 후 userLinkId 목록 캐싱
→ RecommendationCacheService.getUserLinkIds(userId)
   → cache hit: Redis에서 사용자가 저장한 linkId 목록 반환
   → cache miss: DB findLinkIdsByUserId 후 캐싱
→ 필요한 범위의 UserLink만 DB 조회
→ 이미 저장한 linkId 제외
→ cursor pagination
→ 응답
```

관련 파일:

- `RecommendationService.java`
- `RecommendationCacheService.java`
- `RedisConfig.java`

### 4-2. 키워드 검색

대상 API:

```text
GET /api/v1/recommendations/search?keyword={keyword}
```

키워드는 자유 입력이라 ES 결과 자체는 캐싱하지 않는다. 캐시 키가 무한히 늘어날 수 있고 hit율도 낮다.

현재 적용 범위:

- ES 검색은 매 요청 수행
- `userLinkIds`만 캐싱
- 사용자가 저장한 링크 제외는 ES pre-filtering 유지

### 4-3. 최신 공개 링크

대상 흐름:

```text
GET /api/v1/recommendations
```

카테고리가 없는 최신 공개 링크 조회는 아직 Redis 캐싱 대상이 아니다. 트래픽/응답시간 측정 후 `publicRecentUserLinkIds` 같은 짧은 TTL 캐시를 2차로 검토한다.

---

## 5. 캐시 정합성 흐름

추천 캐싱에서 중요한 것은 조회 성능보다 공개 범위 정합성이다. Redis 캐시만 지우거나 Elasticsearch 인덱싱만 갱신하면 stale data가 남을 수 있다.

### 5-1. UserLink 생성

```text
UserLinkService.createUserLink()
→ UserLinkCreatedEvent
→ RecommendationIndexingEventListener.handleUserLinkCreated()
→ userLinkIds:{userId} evict
→ LinkIndexingService.indexUserLink(userLinkId)
   → 공개 Reference + READY Link면 ES add
   → 아니면 ES delete
   → categoryRecommendationIds all evict
```

생성 직후 Link가 아직 `PENDING`이면 추천 인덱싱에서는 삭제/제외 처리된다. 이후 AI 요약이 완료되면 `LinkAiSummaryUpdatedEvent`가 다시 인덱싱을 시도한다.

### 5-2. UserLink 삭제

```text
UserLinkService.deleteUserLink()
→ UserLinkDeletedEvent(userLinkId, userId)
→ RecommendationIndexingEventListener.handleUserLinkDeleted()
→ userLinkIds:{userId} evict
→ LinkIndexingService.deleteUserLink(userLinkId)
   → ES document delete: recommendation-{userLinkId}
   → categoryRecommendationIds all evict
```

삭제 후에는 DB에서 UserLink를 다시 조회할 수 없으므로, `userLinkId`만으로 ES 문서를 삭제하는 전용 메서드를 둔다.

### 5-3. Reference 공개 상태 변경

```text
ReferenceService.updateReference()
→ 변경 전 isPublic 저장
→ reference.update()
→ isPublic이 실제로 바뀐 경우 ReferenceVisibilityChangedEvent 발행
→ RecommendationIndexingEventListener.handleReferenceVisibilityChanged()
→ ReferenceUserLinkRepository.findByReferenceId(referenceId)
→ 연결된 모든 UserLink를 LinkIndexingService.indexUserLink(userLinkId)로 재처리
→ 각 UserLink 상태에 따라 ES add/delete
→ categoryRecommendationIds all evict
```

공개에서 비공개로 변경되면 ES 문서를 삭제하고 Redis 후보 목록도 제거한다. 비공개에서 공개로 변경되면 ES 문서를 추가하고 Redis 후보 목록을 새로 만들 수 있게 지운다.

### 5-4. Link AI 요약 완료

```text
LinkSaveService
→ LinkAiSummaryUpdatedEvent(linkId)
→ RecommendationIndexingEventListener.handleLinkAiSummaryUpdated()
→ LinkIndexingService.indexLink(linkId)
→ 해당 Link를 사용하는 공개 UserLink들을 ES add
→ categoryRecommendationIds all evict
```

추천 인덱싱은 `LinkProcessingStatus.READY`, `title`, `aiSummary`가 모두 있어야 수행된다. 이 이벤트는 PENDING 상태에서 제외됐던 링크를 READY 이후 추천 후보로 올리는 역할을 한다.

---

## 6. 캐시 무효화 매트릭스

| 변경 이벤트 | `userLinkIds` | `categoryRecommendationIds` | Elasticsearch |
|---|---|---|---|
| UserLink 생성 | 해당 userId evict | 인덱싱 결과에 따라 evict | `indexUserLink()` |
| UserLink 삭제 | 해당 userId evict | all evict | `recommendation-{userLinkId}` delete |
| Reference 공개 -> 비공개 | 변경 없음 | all evict | 연결 UserLink delete |
| Reference 비공개 -> 공개 | 변경 없음 | all evict | 연결 UserLink add |
| Link AI 요약 완료 | 변경 없음 | all evict | 해당 Link의 공개 UserLink add |
| 전체 인덱싱 | 변경 없음 | all evict | 공개 UserLink bulk add |

`userLinkIds`는 사용자가 저장한 원본 Link 목록이 바뀔 때만 지운다. Reference 공개 여부 변경은 사용자의 저장 링크 목록 자체를 바꾸지 않으므로 `userLinkIds`를 지우지 않는다.

---

## 7. 캐싱하지 않는 것

### 카테고리 목록

`LinkCategory.getAllDisplayNames()`는 enum 상수 반환이다. Redis 네트워크 왕복보다 JVM 메모리 접근이 빠르므로 캐싱하지 않는다.

### 키워드 검색 결과

키워드는 자유 입력이므로 캐시 키가 폭발하기 쉽다. ES 결과는 캐싱하지 않고 사용자 저장 링크 목록만 캐싱한다.

### 사용자별 최종 추천 응답

최종 응답은 `userId`, `category`, `cursor`, `size`, 공개 상태, 저장 상태에 영향을 받는다. 캐시 키와 무효화 조건이 복잡해져서 현재 단계에서는 캐싱하지 않는다.

### 최신 공개 링크

현재는 캐싱하지 않는다. 실제 트래픽에서 병목이 확인되면 2차 작업으로 검토한다.

---

## 8. 벤치마크 방법

현재 코드에는 `APP_CACHE_RECOMMENDATION_ENABLED` 같은 캐시 ON/OFF 플래그가 없다. 따라서 이 환경변수로 캐시를 끄는 방식은 사용하지 않는다.

현재 비교는 다음 두 가지로 한다.

```text
cold cache
→ Redis 추천 캐시 삭제 후 바로 부하 테스트

warm cache
→ 카테고리별 워밍업 호출 후 부하 테스트
```

직접 실행 예시:

```bash
k6 run \
  -e BASE_URL=http://localhost:8080 \
  -e USERNAME=testuser \
  -e PASSWORD=password123 \
  -e VUS=20 \
  -e DURATION=60s \
  -e WARMUP_REQUESTS=1 \
  k6-scripts/recommendation-category-benchmark.js
```

측정 포인트:

- `http_req_duration` p50/p95
- `rec_category_duration_ms` p50/p95
- 실패율
- Redis cold/warm 차이
- 애플리케이션 로그에서 ES 호출 빈도 감소 여부

캐시 OFF와 비교하려면 캐싱 적용 전 브랜치/커밋을 기준으로 같은 k6 조건에서 별도 측정한다.

---

## 9. 검증 상태

관련 테스트:

```bash
./gradlew test \
  --tests swyp12.team9.server.domain.reference.service.ReferenceServiceTest \
  --tests swyp12.team9.server.domain.recommendation.service.LinkIndexingServiceTest \
  --tests swyp12.team9.server.global.event.IndexingEventListenerTest \
  --tests swyp12.team9.server.domain.reference.service.ReferenceVisibilityIntegrationTest
```

전체 테스트:

```bash
./gradlew test
```

현재 확인 결과:

```text
BUILD SUCCESSFUL
```

---

## 10. 현재 작업 파일

조회 캐싱:

- `RecommendationCacheService.java`
- `RecommendationService.java`
- `RedisConfig.java`

정합성 이벤트:

- `RecommendationIndexingEventListener.java`
- `ReferenceVisibilityChangedEvent.java`
- `UserLinkDeletedEvent.java`
- `ReferenceService.java`
- `UserLinkService.java`

인덱싱:

- `LinkIndexingService.java`
- `UserLinkRepository.java`
- `ReferenceUserLinkRepository.java`

부하 테스트:

- `k6-scripts/recommendation-category-benchmark.js`

---

## 11. 남은 작업 후보

1. 최신 공개 링크 캐싱

트래픽 측정 후 `GET /api/v1/recommendations`가 병목이면 `publicRecentUserLinkIds` 같은 짧은 TTL 캐시를 추가한다.

2. 캐시 hit/miss 메트릭

현재 `RecommendationCacheService`는 source call 카운터로 ES/DB 실제 호출 횟수를 볼 수 있다. 더 명확한 운영 지표가 필요하면 cache hit/miss/bypass 커스텀 메트릭을 추가한다.

3. 카테고리 캐시 부분 무효화

현재 `categoryRecommendationIds`는 추천 인덱싱 변경 시 전체 evict한다. 카테고리가 8개라 단순하고 안전한 방식이다. 데이터가 커지고 인덱싱 변경이 잦아지면 문서 metadata 기반으로 영향 카테고리만 지우는 전략을 검토한다.
