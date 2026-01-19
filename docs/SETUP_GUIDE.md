# Spring AI + Elasticsearch + Kibana 설치 및 사용 가이드

## 📌 목차

1. [설치된 기술 스택 개요](#설치된-기술-스택-개요)
2. [설치 과정](#설치-과정)
3. [현재 구현 상태](#현재-구현-상태)
4. [사용 방법](#사용-방법)
5. [트러블슈팅](#트러블슈팅)

---

## 🎯 설치된 기술 스택 개요

### 1. Spring AI

**역할:** OpenAI API와 연동하여 텍스트를 벡터(숫자 배열)로 변환

**주요 기능:**

- 텍스트 임베딩 (Text → 1536개 숫자)
- ChatGPT 연동 (RAG 챗봇용)
- Elasticsearch와 자동 연결

**버전:** 1.0.0-M5 (Milestone)

---

### 2. Elasticsearch

**역할:** 벡터 검색 엔진 (유사한 글 찾기)

**주요 기능:**

- 벡터 저장 (`dense_vector` 타입)
- 코사인 유사도 계산
- 빠른 검색 (0.1초 이내)

**버전:** 8.17.0  
**포트:** 9200

---

### 3. Kibana

**역할:** Elasticsearch 관리 도구 (GUI)

**주요 기능:**

- 데이터 시각화
- 쿼리 테스트 (Dev Tools)
- 인덱스 관리

**버전:** 8.17.0  
**포트:** 5601

---

### 4. Redis (선택사항 - Phase 3에서 도입)

**역할:** 초고속 메모리 캐시 (성능 최적화)

**주요 기능:**

- 관심사 벡터 캐싱 (0.5초 → 0.01초)
- 최근 활동 기록 임시 저장
- 챗봇 대화 이력 관리

**도입 시점:**

- 추천 API 응답 0.5초 이상
- Phase 3 (성능 최적화) 단계

**현재 상태:** ❌ 미설치 (지금은 필요 없음)

---

## 💡 기술 스택 비유

| 기술              | 역할                  | 비유                            |
| ----------------- | --------------------- | ------------------------------- |
| **MySQL**         | 원본 데이터 영구 보관 | 도서관 (모든 책 보관)           |
| **Elasticsearch** | 벡터 검색 엔진        | 검색대 (빠른 검색)              |
| **Spring AI**     | AI 모델 연동          | 번역기 (텍스트→숫자)            |
| **Kibana**        | ES 관리 도구          | 사서 (도서관 관리)              |
| **Redis**         | 임시 캐시 (선택)      | 책상 위 메모지 (자주 쓰는 것만) |

---

## 🛠️ 설치 과정

### Step 1: Docker Compose 설정

#### 파일: `docker-compose.yml`

```yaml
services:
  mysql:
    image: mysql:8.0
    container_name: swyp-mysql
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: password
      MYSQL_DATABASE: swyp_db
    volumes:
      - mysql-data:/var/lib/mysql

  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.17.0
    container_name: swyp-elasticsearch
    environment:
      - node.name=elasticsearch
      - cluster.name=swyp-cluster
      - discovery.type=single-node
      - xpack.security.enabled=false # 보안 비활성화 (개발용)
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m" # 메모리 제한
    ports:
      - "9200:9200"
    volumes:
      - es-data:/usr/share/elasticsearch/data

  kibana:
    image: docker.elastic.co/kibana/kibana:8.17.0
    container_name: swyp-kibana
    environment:
      - ELASTICSEARCH_HOSTS=http://elasticsearch:9200
      - XPACK_SECURITY_ENABLED=false
    ports:
      - "5601:5601"
    depends_on:
      - elasticsearch

volumes:
  mysql-data:
    driver: local
  es-data:
    driver: local
```

#### 실행 명령어

```bash
# 컨테이너 시작
docker-compose up -d

# 상태 확인
docker-compose ps

# 로그 확인
docker-compose logs -f elasticsearch
docker-compose logs -f kibana

# 중지
docker-compose down
```

---

### Step 2: Gradle 의존성 추가

#### 파일: `build.gradle`

```gradle
dependencies {
    // 기존 의존성...

    // Spring AI
    implementation 'org.springframework.ai:spring-ai-openai-spring-boot-starter'
    implementation 'org.springframework.ai:spring-ai-elasticsearch-store-spring-boot-starter'

    // 기타...
}

dependencyManagement {
    imports {
        mavenBom "org.springframework.ai:spring-ai-bom:1.0.0-M5"
    }
}

repositories {
    mavenCentral()
    maven { url 'https://repo.spring.io/milestone' }  // Spring AI는 마일스톤 저장소 필요
}
```

**주의사항:**

- `repositories`에 `maven { url 'https://repo.spring.io/milestone' }` 필수!
- Spring AI는 아직 정식 릴리즈 전이라 마일스톤 버전 사용

---

### Step 3: Spring 설정 파일

#### 파일: `src/main/resources/application-dev.yaml`

```yaml
spring:
  ai:
    openai:
      api-key: sk-proj-xxxxx... # 본인의 OpenAI API 키
      chat:
        options:
          model: gpt-4o-mini # ChatGPT 모델
      embedding:
        options:
          model: text-embedding-3-small # 임베딩 모델

  elasticsearch:
    uris: http://localhost:9200
```

**OpenAI API 키 발급 방법:**

1. https://platform.openai.com 접속
2. 로그인 → API Keys 메뉴
3. "Create new secret key" 클릭
4. 생성된 키 복사 (한 번만 보여줌!)

---

### Step 4: Elasticsearch 인덱스 생성

#### 방법 1: 터미널 명령어

```bash
curl -X PUT "http://localhost:9200/spring-ai-document-index" \
  -H 'Content-Type: application/json' \
  -d '{
    "mappings": {
      "properties": {
        "embedding": {
          "type": "dense_vector",
          "dims": 1536,
          "index": true,
          "similarity": "cosine"
        },
        "content": {
          "type": "text"
        },
        "metadata": {
          "properties": {
            "title": { "type": "text" },
            "user_id": { "type": "long" }
          }
        }
      }
    }
  }'
```

#### 방법 2: Kibana Dev Tools

1. `http://localhost:5601` 접속
2. 왼쪽 메뉴 → **Management** → **Dev Tools**
3. 아래 코드 입력 후 실행(▶):

```json
PUT /spring-ai-document-index
{
  "mappings": {
    "properties": {
      "embedding": {
        "type": "dense_vector",
        "dims": 1536,
        "index": true,
        "similarity": "cosine"
      },
      "content": {
        "type": "text"
      },
      "metadata": {
        "properties": {
          "title": { "type": "text" },
          "user_id": { "type": "long" }
        }
      }
    }
  }
}
```

---

## ✅ 현재 구현 상태

### 🎯 추천 시스템 기능

**유사 콘텐츠 추천: "이 글과 비슷한 글 추천"**

- 사용자가 특정 링크 클릭 시, 그 링크와 비슷한 다른 링크 추천
- 글 상세 페이지에 "비슷한 링크 추천" 섹션 표시

---

### 1. 인프라 구축 ✅ (100%)

- [x] Docker Compose 환경 구축
- [x] MySQL 8.0 실행 중
- [x] Elasticsearch 8.17.0 실행 중
- [x] Kibana 5601 포트 접속 가능

### 2. Spring AI 통합 ✅ (100%)

- [x] Gradle 의존성 추가
- [x] OpenAI API 키 연동
- [x] Elasticsearch 연결 설정
- [x] 임베딩 모델 설정 (`text-embedding-3-small`)

### 3. 유사 콘텐츠 추천 ✅ (100%)

- [x] `RecommendationService` 생성
  - [x] `seedData()`: 샘플 5개 색인
  - [x] `calculateUserInterest()`: 벡터 계산
  - [x] `recommend()`: 벡터 유사도 검색
- [x] `RecommendationController` 생성
  - [x] `GET /api/recommend/seed`: 데이터 적재
  - [x] `GET /api/recommend?readIds=1`: 유사 글 추천 조회
- [x] Elasticsearch 인덱스 생성
  - [x] `spring-ai-document-index`
  - [x] 벡터 필드 매핑 완료

### ⚠️ 현재 제한사항

#### 1. 목데이터 수준

- ❌ 하드코딩된 샘플 5개만 존재
- ❌ 실제 DB(MySQL)와 연동 안 됨
- ❌ 사용자별 저장 링크 구분 안 됨

#### 2. UI 미구현

- ❌ 글 상세 페이지에 "비슷한 링크 추천" 섹션 없음
- ❌ 링크 저장 기능 없음

---

## 🚧 남은 작업

### Phase 1: 실제 데이터 연동 (필수!) - 2주

**목표:** 사용자가 실제로 링크를 저장하고 유사 글 추천을 받을 수 있게

1. **콘텐츠 엔티티 생성**
   - [ ] `Content` 엔티티 (JPA)
   - [ ] `ContentRepository`
   - [ ] 링크 저장 API 구현

2. **스크래퍼 연동**
   - [ ] 제목, 본문 추출
   - [ ] 저장 시 자동 Elasticsearch 색인

3. **UI 구현**
   - [ ] 링크 저장 기능
   - [ ] 글 상세 페이지에 "비슷한 링크 추천" 섹션
   - [ ] 사용자별 저장 링크 목록

**완료 기준:**

- 사용자가 링크를 붙여넣으면 자동으로 저장 및 색인
- 글 상세 페이지에서 비슷한 글 10개 추천 확인 가능

---

### Phase 2: 성능 최적화 (선택) - 1주

**목표:** 빠르고 안정적인 추천

1. **캐싱 및 최적화**
   - [ ] Redis 캐싱 (벡터 캐싱)
   - [ ] 배치 색인 (1분마다 미색인 글 처리)
   - [ ] 비동기 색인
   - [ ] 추천 품질 대시보드

---

### 📊 진행률

```
전체 설계 기준:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ 100%
████████████████████████░░░░░░░░░░░░░░░░  60%

✅ 인프라 + 유사 글 추천: 60% (완료)
🚧 Phase 1 (실제 데이터 연동): 30% (필수!)
🚧 Phase 2 (성능 최적화): 10% (선택)
```

---

## 📖 사용 방법

### 1. 서버 시작

#### Docker 컨테이너 실행

```bash
cd /Users/yangjinmo/Desktop/server
docker-compose up -d
```

#### Spring Boot 실행

```bash
./gradlew bootRun
```

또는 IntelliJ에서 `ServerApplication` 실행

---

### 2. Elasticsearch 상태 확인

#### 터미널

```bash
# 서버 상태
curl http://localhost:9200

# 인덱스 목록
curl "http://localhost:9200/_cat/indices?v"

# 데이터 개수
curl "http://localhost:9200/spring-ai-document-index/_count"
```

#### 예상 응답

```json
{
  "name": "elasticsearch",
  "cluster_name": "swyp-cluster",
  "version": {
    "number": "8.17.0"
  }
}
```

---

### 3. Kibana 사용법

#### 접속

```
http://localhost:5601
```

#### Dev Tools 사용

1. 왼쪽 메뉴(☰) → **Management** → **Dev Tools**
2. Console 창에서 쿼리 실행

**예시 쿼리:**

```json
# 모든 데이터 조회
GET /spring-ai-document-index/_search
{
  "query": {
    "match_all": {}
  }
}

# 특정 문서 조회
GET /spring-ai-document-index/_doc/1

# 키워드 검색
GET /spring-ai-document-index/_search
{
  "query": {
    "match": {
      "content": "자바"
    }
  }
}

# 인덱스 매핑 확인
GET /spring-ai-document-index/_mapping

# 데이터 삭제
DELETE /spring-ai-document-index/_doc/1
```

---

### 4. Spring AI API 테스트

#### 샘플 데이터 적재

```bash
# 브라우저 또는 curl
curl http://localhost:8080/api/recommend/seed
```

**예상 응답:**

```
Seed data indexed successfully!
```

#### Kibana에서 확인

```json
GET /spring-ai-document-index/_search
{
  "query": {
    "match_all": {}
  }
}
```

**결과:**

- `hits.total.value`: 5
- `hits.hits[0]._source.embedding`: 1536개 숫자 배열

---

#### 추천 조회

```bash
# 읽은 글 ID 1번 기준 추천
curl "http://localhost:8080/api/recommend?readIds=1"
```

**예상 응답:**

```json
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

---

### 5. 벡터 임베딩 직접 테스트

#### Java 코드

```java
@Autowired
private EmbeddingModel embeddingModel;

public void testEmbedding() {
    String text = "자바 스트림 API 성능 최적화";
    float[] vector = embeddingModel.embed(text);

    System.out.println("벡터 차원: " + vector.length);  // 1536
    System.out.println("첫 5개 값: " + Arrays.toString(
        Arrays.copyOfRange(vector, 0, 5)
    ));
}
```

**출력 예시:**

```
벡터 차원: 1536
첫 5개 값: [0.012, -0.334, 0.887, 0.123, -0.456]
```

---

## 🔍 트러블슈팅

### 문제 1: Elasticsearch 연결 실패

**증상:**

```
Connection refused: localhost:9200
```

**해결:**

```bash
# 1. 컨테이너 상태 확인
docker-compose ps

# 2. Elasticsearch 로그 확인
docker-compose logs elasticsearch

# 3. 재시작
docker-compose restart elasticsearch

# 4. 포트 확인
lsof -i :9200
```

---

### 문제 2: Kibana 접속 안 됨

**증상:**

```
Kibana server is not ready yet
```

**해결:**

- Kibana는 시작에 1~2분 소요
- 로그 확인: `docker-compose logs -f kibana`
- `Kibana is now available` 메시지 대기

---

### 문제 3: OpenAI API 에러

**증상:**

```
401 Unauthorized
```

**해결:**

1. API 키 확인

   ```yaml
   spring:
     ai:
       openai:
         api-key: sk-proj-xxxxx # 올바른 키인지 확인
   ```

2. 키 유효성 테스트

   ```bash
   curl https://api.openai.com/v1/models \
     -H "Authorization: Bearer sk-proj-xxxxx"
   ```

3. 사용량 확인
   - https://platform.openai.com/usage
   - 무료 크레딧 소진 여부 확인

---

### 문제 4: 벡터 검색 결과 없음

**증상:**

```
hits.total.value: 0
```

**원인:**

- 데이터가 색인되지 않음
- 인덱스가 없음

**해결:**

```bash
# 1. 인덱스 존재 확인
curl "http://localhost:9200/_cat/indices?v"

# 2. 데이터 적재
curl http://localhost:8080/api/recommend/seed

# 3. 데이터 확인
curl "http://localhost:9200/spring-ai-document-index/_count"
```

---

### 문제 5: Gradle 빌드 실패

**증상:**

```
Could not find spring-ai-bom:1.0.0-M5
```

**해결:**

```gradle
repositories {
    mavenCentral()
    maven { url 'https://repo.spring.io/milestone' }  // 이 줄 필수!
}
```

---

## 💡 유용한 명령어 모음

### Docker 관련

```bash
# 전체 재시작
docker-compose down && docker-compose up -d

# 특정 서비스만 재시작
docker-compose restart elasticsearch

# 로그 실시간 확인
docker-compose logs -f kibana

# 컨테이너 내부 접속
docker exec -it swyp-elasticsearch bash

# 볼륨 삭제 (데이터 초기화)
docker-compose down -v
```

### Elasticsearch 관리

```bash
# 인덱스 삭제
curl -X DELETE "http://localhost:9200/spring-ai-document-index"

# 모든 데이터 조회
curl "http://localhost:9200/spring-ai-document-index/_search?pretty"

# 클러스터 상태
curl "http://localhost:9200/_cluster/health?pretty"

# 노드 정보
curl "http://localhost:9200/_cat/nodes?v"
```

### Kibana 단축키

- **실행**: `Ctrl + Enter` (Windows/Linux), `Cmd + Enter` (Mac)
- **자동완성**: `Ctrl + Space`
- **포맷팅**: `Ctrl + I`

---

## 📚 참고 자료

### 공식 문서

- [Spring AI 공식 문서](https://docs.spring.io/spring-ai/reference/)
- [Elasticsearch 가이드](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html)
- [Kibana 사용법](https://www.elastic.co/guide/en/kibana/current/index.html)
- [OpenAI API 문서](https://platform.openai.com/docs)

### 튜토리얼

- [Spring AI 시작하기](https://spring.io/blog/2023/05/10/introducing-spring-ai)
- [Elasticsearch Vector Search](https://www.elastic.co/guide/en/elasticsearch/reference/current/dense-vector.html)
- [Kibana Dev Tools 가이드](https://www.elastic.co/guide/en/kibana/current/console-kibana.html)

---

## 🎯 다음 단계

### 옵션 1: 추천 시스템 완성 (실용적)

1. MySQL에 `Content` 테이블 만들기
2. 사용자 활동 기록 저장
3. 진짜 관심사 벡터 계산 로직 구현

**→ 실제로 돌아가는 개인화 추천 완성**

### 옵션 2: 챗봇 먼저 (재미있음)

1. 간단한 질문-답변 API 만들기
2. RAG 프롬프트 작성
3. 대화 이력 관리

**→ "내가 저장한 자바 글 찾아줘" 같은 질문에 답변**

### 옵션 3: 현재 상태 테스트 (학습)

1. Kibana에서 데이터 구조 확인
2. `/seed` API로 데이터 넣어보기
3. `/recommend` API 응답 확인

**→ 벡터 검색 원리 체험**

---

## 📞 문의

- **기술 이슈**: 백엔드 팀
- **인프라 문제**: DevOps 팀
- **API 키 관련**: 팀 리더

---

## 📝 체크리스트

### 초기 설정

- [ ] Docker Compose 실행
- [ ] Elasticsearch 접속 확인 (http://localhost:9200)
- [ ] Kibana 접속 확인 (http://localhost:5601)
- [ ] OpenAI API 키 발급 및 설정
- [ ] Gradle 빌드 성공
- [ ] Spring Boot 서버 실행

### 기능 테스트

- [ ] `/api/recommend/seed` 호출 성공
- [ ] Kibana에서 데이터 5개 확인
- [ ] `/api/recommend?readIds=1` 호출 성공
- [ ] 벡터 필드(`embedding`) 확인

### 문제 해결

- [ ] Elasticsearch 로그 확인 방법 숙지
- [ ] Kibana Dev Tools 사용법 숙지
- [ ] OpenAI 사용량 확인 방법 숙지
