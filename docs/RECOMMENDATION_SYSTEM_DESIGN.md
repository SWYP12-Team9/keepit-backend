# Spring AI 기반 유사 콘텐츠 추천 시스템 설계서

## 📌 개요

사용자가 저장한 링크(글)를 기반으로 **AI 벡터 임베딩**과 **Elasticsearch**를 활용한 유사 콘텐츠 추천 시스템

---

## 🎯 추천 시스템 기능

### 유사 콘텐츠 추천

**"이 글과 비슷한 글 추천"**

사용자가 특정 링크를 클릭하면, 그 링크와 내용이 비슷한 다른 링크들을 추천

```
사용자가 "자바 스트림 API" 글 클릭
    ↓
해당 글의 벡터 가져오기
    ↓
Elasticsearch에서 비슷한 벡터 검색
    ↓
"자바 람다", "함수형 프로그래밍" 등 추천
```

---

## 🏗️ 시스템 아키텍처

### 기술 스택

```
프론트엔드: 사용자 앱
     ↓
백엔드: Spring Boot + Spring AI
     ↓
데이터 저장: MySQL (원본) + Elasticsearch (검색/추천)
     ↓
AI 엔진: OpenAI Embedding API
```

### 주요 컴포넌트

- **MySQL**: 사용자 데이터, 글 원본
- **Elasticsearch**: 벡터 검색, 유사도 계산
- **Spring AI**: OpenAI 연동, 벡터 생성
- **Redis** (선택): 벡터 캐싱

---

## 📊 데이터 흐름

### Step 1: 폴더에 링크 저장

```
1. 사용자가 특정 폴더(Reference)를 선택하고 링크 붙여넣기
   예: 폴더 "경제" 선택 → https://velog.io/@user/java-stream

2. 스크래퍼가 내용 추출
   - 제목: "자바 스트림 API 완벽 가이드"
   - 본문: "스트림은 자바 8부터 도입된..."

3. MySQL에 원본 저장

   links 테이블:
   ┌────┬──────────────┬─────────────┬────────────┐
   │ ID │ title        │ content     │ url        │
   ├────┼──────────────┼─────────────┼────────────┤
   │ 1  │ 자바 스트림  │ 스트림은... │ https://.. │
   └────┴──────────────┴─────────────┴────────────┘

   user_links 테이블 (사용자-링크 관계):
   ┌────┬─────────┬─────────┬────────┬─────────┐
   │ ID │ user_id │ link_id │ status │ memo    │
   ├────┼─────────┼─────────┼────────┼─────────┤
   │ 1  │ 123     │ 1       │ UNREAD │ 나중에  │
   └────┴─────────┴─────────┴────────┴─────────┘

   reference_user_links 테이블 (폴더-링크 관계):
   ┌────┬──────────────┬──────────────┐
   │ ID │ reference_id │ user_link_id │
   ├────┼──────────────┼──────────────┤
   │ 1  │ 5 (경제)     │ 1            │
   └────┴──────────────┴──────────────┘

4. OpenAI로 벡터 생성
   텍스트 (제목 + 본문) → [0.12, -0.34, 0.87, ... 1536개 숫자]

5. Elasticsearch에 색인
   {
     "id": "link_1",
     "content": "자바 스트림 API 완벽 가이드. 스트림은 자바 8부터...",
     "embedding": [0.12, -0.34, ...],
     "metadata": {
       "link_id": 1,
       "title": "자바 스트림 API 완벽 가이드",
       "url": "https://..."
     }
   }
```

**소요 시간:** 약 0.5~1초  
**비용:** 글 1개당 약 0.01원

---

### Step 2: 폴더 기반 유사 링크 추천

```
1. 사용자가 특정 폴더(Reference) 선택
   예: "경제" 폴더 클릭

2. 해당 폴더 안의 모든 링크 벡터 가져오기 (MySQL + Elasticsearch)

   MySQL 쿼리:
   SELECT ul.link_id
   FROM reference_user_links rul
   JOIN user_links ul ON rul.user_link_id = ul.id
   WHERE rul.reference_id = 5 (경제 폴더)
   AND ul.user_id = 123

   결과: [link_1, link_3, link_7, link_12]

3. Elasticsearch에서 각 링크의 벡터 조회 후 평균 계산

   link_1: [0.12, -0.34, 0.87, ...]
   link_3: [0.15, -0.30, 0.82, ...]
   link_7: [0.10, -0.38, 0.90, ...]
   link_12: [0.14, -0.32, 0.85, ...]

   평균 벡터 계산:
   avg_vector = [(0.12+0.15+0.10+0.14)/4, (-0.34-0.30-0.38-0.32)/4, ...]
              = [0.1275, -0.335, 0.86, ...]

4. 평균 벡터로 Elasticsearch 유사도 검색
   POST /spring-ai-document-index/_search
   {
     "query": {
       "script_score": {
         "query": { "match_all": {} },
         "script": {
           "source": "cosineSimilarity(params.query_vector, 'embedding') + 1.0",
           "params": {
             "query_vector": [0.1275, -0.335, 0.86, ...]
           }
         }
       }
     },
     "size": 20
   }

5. MySQL에서 이미 저장한 링크 제외 필터링
   - Elasticsearch 결과: [link_1, link_3, link_5, link_7, link_9, ...]
   - 사용자가 이미 저장한 링크: [link_1, link_3, link_7, ...]
   - 최종 추천: [link_5, link_9, ...] (10개)

6. 화면에 "이 폴더와 비슷한 링크 추천" 섹션 표시
   ┌─────────────────────────────────────┐
   │  📌 "경제" 폴더와 비슷한 링크       │
   │                                     │
   │  [썸네일] 부동산 투자 가이드        │
   │  [썸네일] 주식 시장 분석            │
   │  [썸네일] 경제 뉴스 요약            │
   │  ...                                │
   └─────────────────────────────────────┘

⚠️ 초기 사용자 처리 (링크가 하나도 없을 때):
   - 폴더에 링크가 0개면 평균 벡터를 계산할 수 없음
   - 대안 1: 전체 사용자 기준 인기 링크 Top 10 추천
   - 대안 2: 폴더 이름("경제")을 임베딩하여 유사 링크 추천
   - 대안 3: "아직 링크가 없습니다. 첫 링크를 저장해보세요!" 메시지
```

**전체 소요 시간:** 약 0.1초  
**비용:** 검색 1회당 약 0원

---

## ✅ 현재 구현 상태

### 완료된 것

- [x] Docker Compose 환경 (Elasticsearch, Kibana)
- [x] Spring AI 의존성 및 설정
- [x] 벡터 임베딩 생성 기능
- [x] Elasticsearch 색인 기능
- [x] 유사도 검색 API (`/api/recommend?readIds=1`)
- [x] 샘플 데이터 5개로 동작 확인

### 제한사항

- 목데이터 5개만 존재 (실제 DB 연동 필요)
- 사용자별 저장 링크 구분 안 됨
- UI에서 "비슷한 링크 추천" 섹션 미구현

---

## 💾 데이터베이스 설계

### 1. users 테이블

```sql
CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  email VARCHAR(255) UNIQUE NOT NULL,
  name VARCHAR(100),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

### 2. contents 테이블

```sql
CREATE TABLE contents (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,

  -- 원본 데이터
  url VARCHAR(2048) NOT NULL,
  title VARCHAR(500),
  content TEXT,
  summary VARCHAR(1000),

  -- 메타데이터
  author VARCHAR(255),
  published_at TIMESTAMP,
  scraped_at TIMESTAMP,

  -- 인덱스 상태
  indexed_to_es BOOLEAN DEFAULT FALSE,
  es_doc_id VARCHAR(100),

  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  FOREIGN KEY (user_id) REFERENCES users(id),
  INDEX idx_user_id (user_id),
  INDEX idx_indexed (indexed_to_es)
);
```

### 3. Elasticsearch 인덱스 매핑

```json
PUT /spring-ai-document-index
{
  "mappings": {
    "properties": {
      "content": {
        "type": "text"
      },
      "embedding": {
        "type": "dense_vector",
        "dims": 1536,
        "index": true,
        "similarity": "cosine"
      },
      "metadata": {
        "properties": {
          "title": { "type": "text" },
          "user_id": { "type": "long" },
          "url": { "type": "keyword" },
          "created_at": { "type": "date" }
        }
      }
    }
  }
}
```

---

## 🔧 핵심 API 설계

### 1. 샘플 데이터 적재

```
GET /api/recommend/seed

Response:
"Seed data indexed successfully!"
```

### 2. 유사 글 추천

```
GET /api/recommend?readIds=1

Response:
[
  {
    "post": {
      "id": 3,
      "title": "자바 기반의 보안 가이드",
      "embedding": null
    },
    "score": 1.0
  },
  ...
]
```

### 3. 콘텐츠 저장 API (향후 구현)

```
POST /api/contents
Request:
{
  "url": "https://velog.io/@user/java-stream"
}

Response:
{
  "id": 123,
  "title": "자바 스트림 API 완벽 가이드",
  "summary": "스트림은 자바 8부터...",
  "indexed": true,
  "similar_contents": [
    {
      "id": 45,
      "title": "자바 람다 표현식",
      "score": 0.92
    },
    ...
  ]
}
```

### 4. 글 조회 API (향후 구현)

```
GET /api/contents/123

Response:
{
  "id": 123,
  "title": "자바 스트림 API",
  "content": "...",
  "url": "https://...",
  "similar_contents": [...]  // 유사 글 추천
}
```

---

## 🚀 구현 로드맵

### ✅ 완료: 기본 인프라 및 유사 글 추천

- [x] Docker Compose (Elasticsearch, Kibana)
- [x] Spring AI 의존성 및 설정
- [x] 벡터 임베딩 생성
- [x] Elasticsearch 색인
- [x] 유사도 검색 API
- [x] 샘플 데이터 테스트

---

### Phase 1: 실제 데이터 연동 (필수!) - 2주

**목표:** 사용자가 실제로 링크를 저장하고 유사 글 추천을 받을 수 있게

- [ ] Content 엔티티 및 Repository 생성
- [ ] 링크 저장 API 구현
- [ ] 스크래퍼 연동 (제목, 본문 추출)
- [ ] 저장 시 자동 Elasticsearch 색인
- [ ] 글 상세 페이지에 "비슷한 링크 추천" 섹션 추가
- [ ] 사용자별 저장 링크 목록 조회 API

**완료 기준:**

- 사용자가 링크를 붙여넣으면 자동으로 저장 및 색인
- 글 상세 페이지에서 비슷한 글 10개 추천 확인 가능

---

### Phase 2: 성능 최적화 (선택) - 1주

**목표:** 빠르고 안정적인 추천

- [ ] Redis 캐싱 (벡터 캐싱)
- [ ] 배치 색인 (1분마다 미색인 글 처리)
- [ ] 비동기 색인 (사용자는 기다리지 않음)
- [ ] 추천 품질 대시보드

---

## 💰 비용 분석

### OpenAI Embedding API 비용

**가격:** $0.020 / 1M 토큰

### 실사용 시나리오

```
월간 활성 사용자: 1,000명
사용자당 월 저장 글: 20개

1. 글 색인 비용
   - 1,000명 × 20개 = 20,000개/월
   - 글 1개당 2,000자 × 2토큰 = 4,000토큰
   - 총: 80M 토큰
   - 비용: $1.6 (약 2,100원)

2. 검색 쿼리 비용
   - 검색은 이미 저장된 벡터 사용
   - 추가 비용: $0

월 총 비용: 약 2,100원
```

**결론:** 매우 저렴! 커피 한 잔 값

---

## 📊 성과 측정 지표

### 추천 품질 지표

- **클릭률 (CTR)**: "비슷한 링크" 섹션 클릭 비율
- **저장률**: 추천 글을 저장한 비율
- **체류 시간**: 추천 글 평균 읽은 시간

### 목표 수치

```
클릭률: 15% 이상
저장률: 10% 이상
체류 시간: 2분 이상
```

---

## 📚 참고 자료

### 공식 문서

- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [OpenAI Embeddings Guide](https://platform.openai.com/docs/guides/embeddings)
- [Elasticsearch Vector Search](https://www.elastic.co/guide/en/elasticsearch/reference/current/dense-vector.html)

### 실제 사례

- [Netflix Recommendation System](https://netflixtechblog.com/)
- [Spotify Discovery Weekly](https://engineering.atspotify.com/)
- [YouTube Recommendations](https://research.google/pubs/pub45530/)

---

## 📝 변경 이력

| 날짜       | 버전 | 내용                                     |
| ---------- | ---- | ---------------------------------------- |
| 2026-01-18 | 1.0  | 초안 작성                                |
| 2026-01-18 | 1.1  | 1단계(유사 글)/2단계(개인화) 구분 명확화 |
| 2026-01-19 | 2.0  | 유사 콘텐츠 추천에만 집중하도록 단순화   |
