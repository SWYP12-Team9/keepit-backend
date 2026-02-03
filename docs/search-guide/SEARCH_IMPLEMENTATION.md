# 링크 검색 기능 구현 가이드

## 개요

UserLink 검색 기능을 3단계로 구현합니다. 각 단계는 독립적으로 동작하며, 상황에 맞게 선택할 수 있습니다.

## 검색 대상 필드

| 엔티티      | 필드        | 설명     |
|----------|-----------|--------|
| UserLink | why       | 저장 이유  |
| UserLink | memo      | 메모     |
| Link     | title     | 링크 제목  |
| Link     | aiSummary | AI 요약  |
| Link     | url       | 링크 URL |

---

## 단계별 비교

| 항목            | 1단계 (QueryDSL) | 2단계 (Full-Text) | 3단계 (Elasticsearch) |
|---------------|----------------|-----------------|---------------------|
| **성능 (10만건)** | ~2초            | ~50ms           | ~50ms               |
| **한글 형태소 분석** | ❌              | △ (ngram)       | ✅ (nori)            |
| **오타 교정**     | ❌              | ❌               | ✅                   |
| **자동완성**      | ❌              | ✅ (LIKE/Full-Text) | ✅ (ngram)          |
| **동의어 검색**    | ❌              | ❌               | ✅                   |
| **검색어 하이라이팅** | △ (직접 구현)      | △ (직접 구현)       | ✅                   |
| **필드별 가중치**   | ❌              | ✅ (관련도 점수)     | ✅                   |
| **추가 인프라**    | 없음             | 없음              | Elasticsearch       |
| **구현 복잡도**    | 낮음             | 중간              | 높음                  |
| **적합한 상황**    | MVP, 소규모       | 중규모             | 대규모, 고급 검색          |

---

## 1단계: QueryDSL LIKE 검색 (MVP)

### 파일 구조

```
src/main/java/swyp12/team9/server/
├── api/userlink/
│   ├── UserLinkSearchApi.java            # 검색 API 인터페이스
│   ├── UserLinkSearchController.java     # 검색 API 컨트롤러
│   └── dto/
│       ├── request/
│       │   └── UserLinkSearchRequest.java  # 검색 요청 DTO
│       └── response/
│           └── UserLinkSearchResponse.java # 검색 응답 DTO (커서 기반)
└── domain/userlink/
    ├── repository/search/
    │   ├── UserLinkSearchRepository.java        # JPA + Custom 결합
    │   ├── UserLinkSearchRepositoryCustom.java  # Custom 인터페이스
    │   └── UserLinkSearchRepositoryImpl.java    # QueryDSL 구현체
    └── service/search/
        └── UserLinkSearchService.java           # 검색 Service
```

### 주요 코드 (QueryDSL)

```java
// UserLinkSearchRepositoryImpl.java
@RequiredArgsConstructor
public class UserLinkSearchRepositoryImpl implements UserLinkSearchRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<UserLink> searchMyLinks(Long userId, String keyword, String field, Long cursorId, int size) {
        return queryFactory
            .selectFrom(userLink)
            .join(userLink.link, link).fetchJoin()  // N+1 방지
            .where(
                userLink.user.id.eq(userId),
                cursorCondition(cursorId),           // id < cursor
                keywordCondition(keyword, field)     // 동적 필드 검색
            )
            .orderBy(userLink.id.desc())
            .limit(size)
            .fetch();
    }

    // 커서 조건: id < cursorId
    private BooleanExpression cursorCondition(Long cursorId) {
        return cursorId != null ? userLink.id.lt(cursorId) : null;
    }

    // 동적 필드 검색 조건
    private BooleanExpression keywordCondition(String keyword, String field) {
        if (keyword == null || keyword.isEmpty()) return null;
        String lowerKeyword = keyword.toLowerCase();

        if (field != null && !field.isEmpty()) {
            return fieldContains(field, lowerKeyword);  // 특정 필드만
        }

        // 전체 필드 OR 검색
        return containsIgnoreCase(userLink.why, lowerKeyword)
            .or(containsIgnoreCase(userLink.memo, lowerKeyword))
            .or(containsIgnoreCase(link.title, lowerKeyword))
            .or(containsIgnoreCase(link.aiSummary, lowerKeyword))
            .or(containsIgnoreCase(link.url, lowerKeyword));
    }

    // 특정 필드 검색 (switch로 동적 선택)
    private BooleanExpression fieldContains(String field, String keyword) {
        return switch (field) {
            case "why" -> containsIgnoreCase(userLink.why, keyword);
            case "memo" -> containsIgnoreCase(userLink.memo, keyword);
            case "title" -> containsIgnoreCase(link.title, keyword);
            case "aiSummary" -> containsIgnoreCase(link.aiSummary, keyword);
            case "url" -> containsIgnoreCase(link.url, keyword);
            default -> null;
        };
    }

    // 대소문자 무시 LIKE 검색
    private BooleanExpression containsIgnoreCase(StringPath path, String keyword) {
        return path.lower().contains(keyword);
    }
}
```

### API 사용 예시

```bash
# 내 링크 전체 검색 (홈)
GET /api/v1/user-links/search?keyword=Spring&size=20

# 다음 페이지 (커서 사용)
GET /api/v1/user-links/search?keyword=Spring&cursor=10&size=20

# 특정 필드 검색
GET /api/v1/user-links/search?keyword=Spring&field=title&size=20

# 특정 레퍼런스 폴더 내 검색
GET /api/v1/user-links/search?keyword=Spring&referenceId=1&size=20

# 내 모든 레퍼런스 폴더 내 검색
GET /api/v1/user-links/search/references?keyword=Spring&size=20
```

---

## 2단계: MySQL Full-Text Index

### 사전 설정

**1. my.cnf 설정 (한글 검색용)**

```ini
[mysqld]
ngram_token_size=2
```

**2. 마이그레이션 실행**

```sql
-- Full-Text Index 생성
CREATE FULLTEXT INDEX ft_userlink_search
    ON user_links (why, memo)
    WITH PARSER ngram;

CREATE FULLTEXT INDEX ft_link_search
    ON links (title, ai_summary, url)
    WITH PARSER ngram;
```

### 파일 구조

```
src/main/java/.../domain/userlink/
├── repository/search/
│   └── UserLinkFullTextSearchRepository.java  # Full-Text Repository
└── service/search/
    └── UserLinkFullTextSearchService.java     # Full-Text Service

src/main/resources/db/migration/
└── V20260201__add_fulltext_index.sql          # 인덱스 생성 스크립트
```

### 주요 코드 (커서 기반)

```java
// UserLinkFullTextSearchRepository.java
@Query(value = """
        SELECT ul.*,
               (MATCH(ul.why, ul.memo) AGAINST(:keyword IN NATURAL LANGUAGE MODE) +
                MATCH(l.title, l.ai_summary, l.url) AGAINST(:keyword IN NATURAL LANGUAGE MODE)) AS relevance
        FROM user_links ul
        JOIN links l ON ul.link_id = l.link_id
        WHERE ul.user_id = :userId
        AND (:cursorId IS NULL OR ul.user_link_id < :cursorId)
        AND (
            MATCH(ul.why, ul.memo) AGAINST(:keyword IN NATURAL LANGUAGE MODE)
            OR MATCH(l.title, l.ai_summary, l.url) AGAINST(:keyword IN NATURAL LANGUAGE MODE)
        )
        ORDER BY relevance DESC, ul.user_link_id DESC
        LIMIT :size
        """, nativeQuery = true)
List<UserLink> searchByKeywordNaturalLanguageWithCursor(
        Long userId, String keyword, Long cursorId, int size);
```

### 설정 (application.yml)

```yaml
search:
  mode: fulltext  # 'like' 또는 'fulltext'
  boolean-mode: false  # Boolean Mode 사용 여부
```

### Boolean Mode 검색어

```java
// Boolean 검색어 예시
"+Spring +Boot"      // Spring AND Boot
"+Spring -Legacy"    // Spring 포함, Legacy 제외
"\"Spring Boot\""    // 정확히 "Spring Boot"
"Spring*"            // Spring으로 시작
```

### 자동완성 (Autocomplete)

2단계에서 자동완성 기능을 지원합니다. LIKE prefix 검색과 Full-Text 검색 두 가지 방식을 제공합니다.

**Repository 메서드**

```java
// 1. LIKE prefix 검색 (시작 부분 일치) - 가장 빠름
@Query(value = """
    SELECT DISTINCT l.title
    FROM user_links ul
    JOIN links l ON ul.link_id = l.link_id
    WHERE ul.user_id = :userId
    AND LOWER(l.title) LIKE LOWER(CONCAT(:prefix, '%'))
    ORDER BY l.title
    LIMIT 10
    """, nativeQuery = true)
List<String> findTitlesByPrefix(Long userId, String prefix);

// 2. LIKE 부분 문자열 검색 (어느 위치든 매칭)
@Query(value = """
    SELECT DISTINCT l.title
    FROM user_links ul
    JOIN links l ON ul.link_id = l.link_id
    WHERE ul.user_id = :userId
    AND LOWER(l.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
    ORDER BY
        CASE WHEN LOWER(l.title) LIKE LOWER(CONCAT(:keyword, '%')) THEN 0 ELSE 1 END,
        l.title
    LIMIT 10
    """, nativeQuery = true)
List<String> findTitlesByKeyword(Long userId, String keyword);

// 3. Full-Text Boolean Mode 검색 (형태소 분석 적용)
@Query(value = """
    SELECT DISTINCT l.title
    FROM user_links ul
    JOIN links l ON ul.link_id = l.link_id
    WHERE ul.user_id = :userId
    AND MATCH(l.title) AGAINST(CONCAT(:prefix, '*') IN BOOLEAN MODE)
    ORDER BY MATCH(l.title) AGAINST(CONCAT(:prefix, '*') IN BOOLEAN MODE) DESC
    LIMIT 10
    """, nativeQuery = true)
List<String> findTitlesByPrefixFullText(Long userId, String prefix);

// 4. 도메인 자동완성
@Query(value = """
    SELECT DISTINCT
        SUBSTRING_INDEX(SUBSTRING_INDEX(REPLACE(REPLACE(l.url, 'https://', ''), 'http://', ''), '/', 1), '?', 1) AS domain
    FROM user_links ul
    JOIN links l ON ul.link_id = l.link_id
    WHERE ul.user_id = :userId
    AND l.url LIKE CONCAT('%', :prefix, '%')
    ORDER BY domain
    LIMIT 10
    """, nativeQuery = true)
List<String> findDomainsByPrefix(Long userId, String prefix);
```

**자동완성 방식 비교**

| 방식 | 메서드 | 장점 | 단점 |
|------|--------|------|------|
| LIKE prefix | `findTitlesByPrefix` | 인덱스 활용, 가장 빠름 | 시작 부분만 매칭 |
| LIKE contains | `findTitlesByKeyword` | 유연한 매칭 | 인덱스 미사용, 느림 |
| Full-Text | `findTitlesByPrefixFullText` | 형태소 분석 적용 | 2글자 이상 필요 |
| 도메인 | `findDomainsByPrefix` | URL에서 도메인 추출 | 정규화 필요 |

---

## 3단계: Elasticsearch

### 사전 설정

**1. Elasticsearch 설치 및 실행**

```bash
# Docker로 실행
docker run -d --name elasticsearch \
  -p 9200:9200 -p 9300:9300 \
  -e "discovery.type=single-node" \
  -e "xpack.security.enabled=false" \
  docker.elastic.co/elasticsearch/elasticsearch:8.11.0

# nori 플러그인 설치 (한글 분석)
docker exec -it elasticsearch bin/elasticsearch-plugin install analysis-nori
docker restart elasticsearch
```

**2. 의존성 추가 (build.gradle)**

```gradle
implementation 'org.springframework.boot:spring-boot-starter-data-elasticsearch'
```

### 파일 구조

```
src/main/java/.../domain/userlink/elasticsearch/
├── UserLinkDocument.java                    # ES Document
├── UserLinkElasticsearchRepository.java     # ES Repository
└── UserLinkElasticsearchService.java        # ES Service

src/main/java/.../global/config/
└── ElasticsearchConfig.java                 # ES 설정

src/main/resources/elasticsearch/
└── userlinks-settings.json                  # 인덱스 설정 (분석기 등)
```

### 주요 기능

**1. 필드별 가중치**

```java
// title 매칭 = 3점, aiSummary/why = 2점, 나머지 = 1점
"fields":["title^3","aiSummary^2","why^2","memo","url"]
```

**2. 오타 교정 (Fuzzy)**

```java
"fuzziness":"AUTO"
// "Sprign" → "Spring" 자동 매칭
```

**3. 한글 형태소 분석 (Nori)**

```json
{
  "analyzer": {
    "nori": {
      "type": "custom",
      "tokenizer": "nori_tokenizer",
      "filter": [
        "nori_readingform",
        "lowercase"
      ]
    }
  }
}
// "개발자" 검색 시 "개발", "개발하다" 등 매칭
```

**4. 동의어 검색**

```json
{
  "filter": {
    "synonym_filter": {
      "type": "synonym",
      "synonyms": [
        "개발,프로그래밍,코딩",
        "프론트엔드,frontend,FE",
        "백엔드,backend,BE"
      ]
    }
  }
}
```

**5. 자동완성**

```java
// "Spr" 입력 시 "Spring Boot", "Sprint" 등 제안
Page<UserLinkDocument> autocomplete(Long userId, String prefix, Pageable pageable);
```

**6. 검색어 하이라이팅**

```java
// "Spring" 검색 시
// 원본: "Spring Boot 가이드"
// 결과: "<em>Spring</em> Boot 가이드"
```

### 설정 (application.yml)

```yaml
elasticsearch:
  host: localhost
  port: 9200
  scheme: http
  # username: elastic  # 인증 필요시
  # password: password

search:
  mode: elasticsearch  # 'like', 'fulltext', 'elasticsearch'
```

### 데이터 동기화

```java
// UserLink 생성/수정 시 자동 동기화 (비동기)
@Async
public void indexUserLink(UserLink userLink) {
    UserLinkDocument document = UserLinkDocument.from(userLink);
    elasticsearchRepository.save(document);
}

// 전체 재색인 (관리자용)
public void reindexAll() {
    List<UserLink> all = userLinkRepository.findAll();
    elasticsearchRepository.saveAll(all.stream()
            .map(UserLinkDocument::from)
            .toList());
}
```

---

## API 엔드포인트

### 검색 API

| 메서드   | 엔드포인트                              | 설명                     | 인증       |
|-------|------------------------------------|------------------------|----------|
| `GET` | `/api/v1/user-links/search`        | 링크 검색 (홈/레퍼런스)         | Required |
| `GET` | `/api/v1/user-links/search/references` | 내 모든 레퍼런스 폴더 내 검색 | Required |

### 검색 Request Parameters

| 파라미터       | 타입      | 필수 | 설명                                       |
|------------|---------|----|------------------------------------------|
| keyword    | String  | ✅  | 검색어 (2~50자)                              |
| field      | String  | ❌  | 검색 필드 (why, memo, title, aiSummary, url) |
| referenceId | Long   | ❌  | 레퍼런스 폴더 ID (지정 시 해당 폴더 내 검색)          |
| cursor     | String  | ❌  | 커서 (첫 요청 시 null)                         |
| size       | Integer | ❌  | 페이지 크기 (기본: 20, 최대: 50)                  |

### 검색 Response (커서 기반)

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "items": [
      {
        "id": 1,
        "title": "Spring Boot 가이드",
        "url": "https://example.com/spring",
        "aiSummary": "Spring Boot에 대한 종합 가이드",
        "why": "개발 공부용",
        "memo": "핵심 내용 정리",
        "thumbnailUrl": "https://example.com/thumb.jpg",
        "viewCount": 10,
        "matchedFields": [
          "title",
          "why"
        ]
      }
    ],
    "keyword": "Spring",
    "nextCursor": "5",
    "hasNext": true
  }
}
```

---

## 단계별 전환 가이드

### 1단계 → 2단계 전환

1. `V20260201__add_fulltext_index.sql` 마이그레이션 실행
2. `application.yml`에서 `search.mode: fulltext` 설정
3. (선택) `my.cnf`에 `ngram_token_size=2` 설정 후 MySQL 재시작

### 2단계 → 3단계 전환

1. Elasticsearch 설치 및 nori 플러그인 설치
2. `build.gradle`에 `spring-boot-starter-data-elasticsearch` 추가
3. `application.yml`에 Elasticsearch 연결 정보 설정
4. 전체 재색인 실행: `elasticsearchService.reindexAll()`
5. `search.mode: elasticsearch` 설정

---

## QueryDSL vs Native Query 비교

| 항목 | Native Query | QueryDSL |
|------|-------------|----------|
| 타입 안전성 | ❌ 문자열 기반 | ✅ 컴파일 타임 체크 |
| 리팩토링 | ❌ 필드명 변경 시 수동 수정 | ✅ 자동 감지 |
| 동적 쿼리 | ⚠️ CASE WHEN 필요 | ✅ switch/if 사용 |
| N+1 방지 | ⚠️ 별도 쿼리 필요 | ✅ fetchJoin() 지원 |
| 가독성 | ⚠️ SQL 문자열 | ✅ 메서드 체이닝 |

---

## 성능 최적화 팁

### 1단계 (QueryDSL LIKE)

- 검색 필드에 일반 인덱스 추가 (효과 제한적)
- 결과 캐싱 (Redis)
- fetchJoin()으로 N+1 문제 방지

### 2단계 (Full-Text)

- `innodb_ft_min_token_size` 조정
- `innodb_ft_result_cache_limit` 증가
- 자동완성: prefix 검색 시 title 컬럼에 일반 인덱스 추가
- 자동완성: 결과 캐싱 (Redis) - 같은 prefix에 대해 캐시 활용

### 3단계 (Elasticsearch)

- 샤드 수 조정 (데이터 크기에 따라)
- 레플리카 수 조정 (가용성/성능 균형)
- 검색 결과 캐싱
- Bulk API로 배치 색인