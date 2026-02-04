# Elasticsearch 검색 구현 (3단계)

## 개요

이 폴더에는 Elasticsearch 검색 구현에 필요한 예제 코드가 포함되어 있습니다.
실제 프로젝트에 적용하려면 아래 단계를 따르세요.

## 적용 단계

### 1. 의존성 추가 (build.gradle)

```gradle
dependencies {
    // Elasticsearch
    implementation 'org.springframework.boot:spring-boot-starter-data-elasticsearch'
}
```

### 2. Elasticsearch 설치

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

### 3. 설정 추가 (application.yml)

```yaml
elasticsearch:
  host: localhost
  port: 9200
  scheme: http
```

### 4. 파일 복사

이 폴더의 파일들을 프로젝트에 복사하세요:

```
docs/elasticsearch-example/
├── UserLinkDocument.java          → domain/userlink/elasticsearch/
├── UserLinkElasticsearchRepository.java → domain/userlink/elasticsearch/
├── UserLinkElasticsearchService.java    → domain/userlink/elasticsearch/
├── ElasticsearchConfig.java       → global/config/
└── userlinks-settings.json        → resources/elasticsearch/
```

### 5. 전체 재색인

```java
@Autowired
private UserLinkElasticsearchService elasticsearchService;

// 전체 데이터 색인
elasticsearchService.reindexAll();
```

## 파일 목록

| 파일 | 설명 |
|------|------|
| `UserLinkDocument.java` | Elasticsearch Document 엔티티 |
| `UserLinkElasticsearchRepository.java` | ES Repository (쿼리 포함) |
| `UserLinkElasticsearchService.java` | ES Service (검색, 동기화) |
| `ElasticsearchConfig.java` | ES 클라이언트 설정 |
| `userlinks-settings.json` | 인덱스 설정 (nori 분석기 등) |