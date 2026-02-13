# Link URL 중복 방지 및 동시성 처리 개선

## 개요

Link 테이블의 URL 중복 저장 문제를 해결하고, 동시 요청 시 커넥션 풀 고갈 위험을 제거하기 위한 리팩토링.

## 문제점

### 1. URL prefix 인덱스의 한계

기존 `url(500)` prefix UNIQUE 인덱스는 URL의 앞 500자만 비교하여, 앞 500자가 동일하고 뒤쪽만 다른 URL에서 충돌 가능성이 존재했음.

### 2. @Transactional 내 DataIntegrityViolationException catch 시 트랜잭션 오염

`LinkService.createLink()`가 기본 전파(`REQUIRED`)로 `UserLinkService`와 같은 트랜잭션에 참여.
동시 요청으로 `DataIntegrityViolationException` 발생 시:

1. Hibernate Session이 inconsistent 상태가 됨
2. 트랜잭션이 rollback-only로 마킹됨
3. catch 블록 이후 모든 DB 작업 실패

### 3. 동시 요청 시 UserLink 중복 생성

UserLink 중복 체크가 `link != null`일 때만 수행되어, `link == null` → catch 블록에서 Link를 재사용하는 경우 중복 체크를 건너뜀.

```
스레드 A (유저 1, URL X)          스레드 B (유저 1, URL X)
─────────────────────           ─────────────────────
findByUrlHash() → null          findByUrlHash() → null

link == null → 중복 체크 스킵     link == null → 중복 체크 스킵

createLink() → 성공              createLink() → catch → 기존 Link 재사용

UserLink 생성 ✅                 UserLink 생성 ✅  ← 중복!
```

### 4. 커넥션 풀 고갈 위험

`getOrCreateLink()`에 `@Transactional(REQUIRES_NEW)`가 걸려있는 상태에서, 트랜잭션 안에서 느린 외부 호출 2개가 실행됨:

- `ScrapingService.scrapeUrl()` — 외부 HTTP 호출 (2~3초)
- `LinkAiService.summarizeLink()` — AI API 호출 (3~5초)

이로 인해:
- DB 커넥션을 5~8초 동안 점유하지만, 실제 DB 작업은 수십ms
- 외부 트랜잭션 A + REQUIRES_NEW 트랜잭션 B로 **커넥션 2개 동시 점유**
- HikariCP 기본 풀 사이즈 10개 기준, 동시 요청 증가 시 커넥션 풀 고갈

```
기존:
  커넥션 1 (트랜잭션 A): ──────────── 5~8초 ────────────
  커넥션 2 (REQUIRES_NEW): ──────── 5~8초 ────────────
                            스크래핑    AI     save
```

## 해결

### 1. url_hash(SHA-256) 컬럼 추가

전체 URL에 대한 SHA-256 해시를 저장하여 완전한 UNIQUE 보장.

- `Link.java`: `urlHash` 필드 + `generateUrlHash()` 메서드 추가
- `LinkRepository.java`: `findByUrlHash()`, `existsByUrlHash()` 추가
- 마이그레이션: `url_hash` 컬럼 + `uk_link_url_hash` UNIQUE 인덱스

### 2. ConcurrentHashMap URL 단위 락

예외 기반 catch-and-retry 패턴 대신, `ConcurrentHashMap` + `synchronized`로 URL 단위 메모리 락 적용.

```
스레드 A: lock 획득 → find → null → create+COMMIT → lock 해제
스레드 B: lock 대기 →              lock 획득 → find → 있음 → 재사용 → lock 해제
                                  (스크래핑+AI 중복 수행 없음)
```

**REQUIRES_NEW가 여전히 필요한 이유:**

synchronized 락은 코드 실행 순서만 보장하고, DB 커밋 시점은 보장하지 않음.
REQUIRES_NEW로 독립 트랜잭션에서 실행해야 메서드 리턴 = 커밋 완료이므로, 다음 스레드가 커밋된 데이터를 확실히 조회 가능.

```
REQUIRED (기본값):
  스레드 A: lock 획득 → save() (커밋 안 됨) → lock 해제
  스레드 B: lock 획득 → find() → null ← A가 아직 커밋 안 함!

REQUIRES_NEW:
  스레드 A: lock 획득 → save() → COMMIT ✅ → lock 해제
  스레드 B: lock 획득 → find() → 있음 ✅
```

### 3. UserLink 중복 체크 위치 이동

Link 확보 이후로 중복 체크를 이동하여, 모든 경로에서 검사.

```
기존: 중복체크 → Link 생성/재사용 → UserLink 생성 (체크 누락 가능)
수정: Link 생성/재사용 → 중복체크 → UserLink 생성 (항상 검사)
```

### 4. LinkSaveService 분리로 커넥션 점유 최소화

외부 호출(스크래핑, AI 요약)을 트랜잭션 밖으로 분리하고, DB 저장만 짧은 REQUIRES_NEW 트랜잭션으로 처리.

- `LinkSaveService.findOrSaveLink()` — REQUIRES_NEW, find + save만 수행 (수십ms)
- `LinkService.getOrCreateLink()` — 트랜잭션 없음, 외부 호출 후 저장 위임

```
수정 후:
  커넥션 1 (트랜잭션 A): ──────────── 5~8초 ────────────
  커넥션 2 (REQUIRES_NEW):                     ─ 수십ms ─
                            스크래핑    AI     find+save
                           (트랜잭션 밖)       (트랜잭션 안)
```

`findOrSaveLink()` 내부에서 find를 한 번 더 수행하여, MySQL REPEATABLE READ 스냅샷 문제도 방지.

## 수정된 파일

| 파일 | 변경 내용 |
|------|----------|
| `Link.java` | `urlHash` 필드, `generateUrlHash()` 메서드 추가 |
| `LinkRepository.java` | `findByUrlHash()`, `existsByUrlHash()` 추가 |
| `LinkService.java` | 트랜잭션 제거, 외부 호출 분리, `LinkSaveService` 위임 |
| `LinkSaveService.java` | 신규 생성, REQUIRES_NEW find+save |
| `UserLinkService.java` | ConcurrentHashMap 락, UserLink 중복 체크 위치 이동 |
| `LinkNotFoundException.java` | 신규 생성 |
| `V20260212__add_unique_url_constraint.sql` | url_hash 컬럼 + UNIQUE 인덱스 마이그레이션 |

## 제약 사항

- **단일 인스턴스 전용**: ConcurrentHashMap 락은 JVM 단위이므로 다중 인스턴스 환경에서는 분산 락(Redis Lock 등)으로 교체 필요
- DB UNIQUE 제약(`uk_link_url_hash`)은 다중 인스턴스 안전망으로 유지

## 프론트 방어만으로 충분하지 않은 이유

프론트에서 링크 생성 버튼 클릭 시 로딩 스피너를 표시하여 중복 클릭을 방지하고 있지만, 이는 **UI 레벨 방어**이고 백엔드의 락 + UNIQUE 제약은 **데이터 레벨 보장**이다. 역할이 다르다.

### 프론트 방어만으로 막을 수 없는 케이스

- **다른 사용자**가 같은 URL을 동시에 저장 (스피너와 무관)
- 네트워크 지연으로 **재시도**(retry) 발생
- API를 **직접 호출** (Postman, curl, 스크립트 등)
- 브라우저 **뒤로가기 후 재제출**
- 스피너 로직의 **버그** (상태 초기화 누락 등)

프론트 방어는 "대부분의 일반 사용자"를 커버하지만, 서버는 "모든 가능한 요청"을 커버해야 한다.

### 각 계층의 역할

```
프론트 (로딩 스피너)       → 사용자 경험 개선, 대부분의 중복 클릭 방지
백엔드 (ConcurrentHashMap) → 같은 서버 내 동시 요청 직렬화
백엔드 (DB UNIQUE 제약)    → 최종 안전망, 어떤 상황에서도 중복 데이터 불가
```

프론트 방어가 잘 되어 있으면 백엔드의 동시성 로직이 실제로 실행될 일은 거의 없지만, **실행될 때 정확히 동작하는 것**이 중요하다. 데이터 정합성은 결국 백엔드의 책임이다.

## 리팩토링 효과: 응답 속도 vs 서버 안정성

이번 리팩토링으로 사용자가 체감할 수 있는 개별 응답 속도 차이는 거의 없다.
링크 저장 시 기다리는 시간의 대부분은 스크래핑 + AI 요약이며, 이 외부 호출 시간(5~8초)은 리팩토링 전후 동일하다.

```
기존:    스크래핑(3초) + AI(5초) + DB저장(10ms) = ~8초
수정 후: 스크래핑(3초) + AI(5초) + DB저장(10ms) = ~8초
                                                 ↑ 사용자 체감 동일
```

이번 리팩토링이 개선하는 것은 개별 응답 속도가 아니라, **동시 사용자가 많아졌을 때 서버가 버티는 능력**이다.

| | 기존 | 수정 후 |
|---|---|---|
| 사용자 1명 응답 속도 | ~8초 | ~8초 (동일) |
| 동시 10명 요청 시 | 커넥션 20개 점유 → **풀 고갈, 전체 서비스 장애** | 커넥션 10개 + 순간 1개 → **정상 동작** |
| 같은 URL 동시 요청 | 스크래핑+AI 중복 수행 후 1개 버림 | 1번만 수행, 나머지는 재사용 |

즉, **개별 응답 속도 개선이 아니라 서버 안정성과 리소스 효율성 개선**이 목적이다.

## 관련 문서

- [REQUIRES_NEW가 필요한 이유](REQUIRES_NEW_EXPLANATION.md) — synchronized 락만으로 부족한 이유와 REQUIRES_NEW의 역할
