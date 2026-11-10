# Keepit 링크 저장의 락 변천사

링크 저장 흐름에서 락이 왜 필요했고, 서버 내부 락에서 Redis 분산 락으로 어떻게 바뀌어 왔는지 정리한다.
"바뀌었다"기보다 **처리 구조가 바뀌면서 지켜야 할 구간이 늘어났고, 늘어난 구간에 맞는 락을 하나씩 더했다**가 정확하다. 현재 코드에는 세 장치가 모두 남아 있다.

---

## 0. 락을 고르는 세 질문

| 질문 | 의미 |
|---|---|
| 어디에 있나 | 락 정보를 누가 볼 수 있나 (한 서버 메모리 / DB / Redis) |
| 누가 푸나 | 건 쪽만 풀 수 있나, 다른 쪽도 풀 수 있나 |
| 언제까지 살아야 하나 | 한 요청 안에서 끝나나, 요청이 끝난 뒤에도 유지돼야 하나 |

| | 서버 내부 락 (`synchronized`, `ReentrantLock`) | DB 락 (비관적/낙관적/UNIQUE) | Redis 락 (`SET NX EX`) |
|---|---|---|---|
| 어디에 | 그 JVM 메모리 | DB | Redis |
| 누가 푸나 | 건 스레드만 | 그 트랜잭션 (커밋/롤백 시) | 키를 아는 누구나 |
| 수명 | 코드 블록 / 프로세스 | 트랜잭션 | TTL 또는 명시적 삭제 |
| 재시작 시 | 사라짐 | 트랜잭션과 함께 정리 | 남아 있음 (TTL로 정리) |

---

## 1단계 — 동기 처리 시절: 락 없음 + 예외로 수습

**구조**: 요청 하나가 트랜잭션 안에서 `조회 → 스크래핑(2~3초) → AI 요약(3~5초) → 저장`까지 전부 수행.

**문제** ([LINK_DUPLICATE_FIX.md](LINK_DUPLICATE_FIX.md))
- 같은 URL을 동시에 저장하면 둘 다 "없음"을 보고 스크래핑·AI를 **두 번** 돌린다. 뒤에 저장하는 쪽은 UNIQUE 위반 예외로 실패.
- 그 예외를 같은 트랜잭션 안에서 catch하면 트랜잭션이 rollback-only로 오염된다.
- `url(500)` prefix UNIQUE라 앞 500자가 같은 URL은 구분하지 못한다.
- 외부 호출 동안 DB 커넥션을 5~8초 붙잡는다 (`REQUIRES_NEW`까지 겹쳐 요청당 2개).

이 시점에 필요한 건 "같은 URL에 대해 조회~저장을 한 줄로 세우기"였다.

---

## 2단계 — 서버 내부 락 도입 (#90)

**변경**
1. `url_hash`(SHA-256) 컬럼 + `uk_link_url_hash` UNIQUE → **정확성의 최종 보장**
2. URL 해시 단위 `ConcurrentHashMap` + `synchronized` 락 → **중복 스크래핑·AI 방지**
3. 외부 호출을 트랜잭션 밖으로, 저장만 `LinkSaveService`의 짧은 `REQUIRES_NEW` 트랜잭션으로 → **커넥션 점유 최소화**

```
스레드 A: lock 획득 → find → 없음 → 스크래핑+AI → save+COMMIT → lock 해제
스레드 B: lock 대기 ──────────────────────────────→ lock 획득 → find → 있음 → 재사용
```

**왜 DB 락이 아니라 서버 내부 락이었나**
- 지켜야 할 구간에 **5~8초짜리 외부 호출**이 들어 있었다. DB 비관적 락(`SELECT ... FOR UPDATE`)으로 막으려면 그동안 트랜잭션과 커넥션을 열어 둬야 하는데, 이게 바로 고치려던 커넥션 고갈 문제다.
- 게다가 아직 없는 행은 `FOR UPDATE`로 잠글 대상이 없다 (갭 락에 기대야 하고 데드락 위험이 생긴다).
- 당시 서버는 한 대였고, 락을 거는 쪽과 푸는 쪽이 같은 요청 스레드, 수명도 한 요청 안이었다. 세 질문 모두 서버 내부 락으로 충분했다.

**왜 `REQUIRES_NEW`가 함께 필요했나**
`synchronized`는 실행 순서만 보장하고 커밋 시점은 보장하지 않는다. A가 커밋 전에 락을 풀면 B는 여전히 "없음"을 본다. 독립 트랜잭션이어야 "메서드 리턴 = 커밋 완료"가 된다. ([REQUIRES_NEW_EXPLANATION.md](REQUIRES_NEW_EXPLANATION.md))

**문서에 이미 적어 둔 한계**: JVM 단위라 다중 인스턴스에서는 못 막는다. 그래서 DB UNIQUE를 안전망으로 남겼다.

---

## 3단계 — Redis Stream 비동기 처리 + Redis 분산 락 (#104)

**구조가 바뀜**: 스크래핑·AI를 요청에서 떼어냈다.

```
[요청 스레드]  UserLinkService.createUserLink
   └ synchronized(urlHash) { placeholder Link 저장 (REQUIRES_NEW, 수십 ms) }  ← 2단계 락, 범위가 짧아짐
   └ 트랜잭션 종료 → LinkCreatedEvent

[@Async 리스너 스레드]  LinkEventListener (AFTER_COMPLETION, fallbackExecution)
   └ findById(linkId) — 행이 있을 때만 진행 (롤백된 새 링크는 여기서 걸러짐)
   └ LinkStreamProducer.publishLinkProcessTask
        1. SADD link:notify_users:{linkId} userId       ← 대기자 등록 (항상)
        2. SET link:processing_lock:{linkId} NX EX 30분  ← 분산 락
           성공 → Stream에 작업 발행
           실패 → 이미 진행 중, TTL만 연장하고 합류

[Stream 컨슈머 스레드]  LinkStreamConsumer
   └ 스크래핑 → (heartbeat) → AI 요약 → (heartbeat) → DB 반영
   └ Lua: 대기자 전원 꺼내기 + 락/백업 키 삭제 → 대기자 전원에게 SSE
```

코드 위치
- 락 획득: [LinkProcessStreamGateway.java:46](../src/main/java/swyp12/team9/server/domain/link/infrastructure/LinkProcessStreamGateway.java:46) — `setIfAbsent(key, "PROCESSING", ttl)`
- 대기자 → 락 순서: [LinkStreamProducer.java:25](../src/main/java/swyp12/team9/server/domain/link/service/LinkStreamProducer.java:25)
- heartbeat(TTL 연장) Lua: [LinkProcessStreamGateway.java:76](../src/main/java/swyp12/team9/server/domain/link/infrastructure/LinkProcessStreamGateway.java:76)
- 해제(drain) Lua: [LinkProcessStreamGateway.java:97](../src/main/java/swyp12/team9/server/domain/link/infrastructure/LinkProcessStreamGateway.java:97)

### 지켜야 할 구간이 달라졌다

2단계의 `synchronized`는 여전히 있지만, 이제 그 안에는 placeholder 저장(수십 ms)만 있다. 비싼 작업은 락 밖, 다른 스레드에서 돈다. 그래서 "같은 링크의 스크래핑·AI를 한 번만"을 지키려면 **작업 발행 ~ 컨슈머 완료** 구간을 덮는 새 락이 필요해졌다.

### 왜 서버 내부 락으로는 안 되나 — 세 질문으로

| 질문 | 이 구간의 요구 | 서버 내부 락 |
|---|---|---|
| 누가 푸나 | 거는 쪽은 리스너 스레드, 푸는 쪽은 컨슈머(또는 recovery) 스레드 | `synchronized`/`ReentrantLock`은 건 스레드만 해제 가능 → ✗ |
| 언제까지 | 요청이 끝난 뒤에도, 재시도·recovery로 수 분까지 | 블록을 벗어나면 끝 → ✗ |
| 어디에 | 작업 자체(Stream 메시지)가 Redis에 영속됨 | JVM 메모리 → 재시작하면 "처리 중" 표시만 사라지고 Stream pending 메시지는 남음 → 불일치 |

마지막 줄이 핵심이다. 작업 큐가 Redis에 있으니 "이 작업이 진행 중"이라는 표시도 같은 곳에 둬야 서버가 재시작돼도 둘이 어긋나지 않는다. 다중 서버 대응은 덤이다.

### 왜 DB 락으로는 안 되나

placeholder 저장 트랜잭션은 이미 커밋돼 끝났다. DB 락으로 이 구간을 덮으려면 수 초~수 분 동안 트랜잭션과 커넥션을 열어 둬야 한다 — 2단계에서 없앤 문제를 되살리는 셈이다.

### 락 하나로 끝나지 않은 부분: 대기자 관리

락에 실패한 요청을 그냥 버리면 그 사용자는 완료 알림(SSE)을 못 받는다. 그래서
1. **락보다 먼저** 대기자 Set에 사용자를 넣는다. 순서가 반대면 "락 실패 → (그 사이 컨슈머가 완료·정리) → 대기자 등록"이 되어 알림이 영영 안 간다.
2. 완료 시 `SMEMBERS` + `DEL`을 Lua 한 번에 실행한다. 둘 사이에 끼어든 사용자가 조회에서도 빠지고 삭제로도 사라지는 일을 막기 위해서다.

### 롤백된 요청은 어떻게 걸러지나

"커밋 후 이벤트라 롤백된 요청은 등록되지 않는다"는 설명은 **틀리다.**
- `AFTER_COMPLETION`은 커밋이든 롤백이든 트랜잭션이 끝나면 실행된다. 커밋만 받으려면 `AFTER_COMMIT`이어야 한다.
- `fallbackExecution = true`라 트랜잭션 밖에서 발행된 이벤트도 즉시 실행된다. 이미 있는 링크를 다시 저장하는 경로([LinkService.getOrCreateLink](../src/main/java/swyp12/team9/server/domain/link/service/LinkService.java))가 이쪽이다.
- 실제로 롤백된 새 링크가 Stream에 안 올라가는 이유는 리스너가 `linkRepository.findById(linkId).ifPresent(...)`로 **DB를 다시 확인**하기 때문이다. 롤백됐으면 행이 없다.

이론상 차이: 이벤트 단계로 거르면 "커밋된 것만" 보장되고, 재조회로 거르면 "지금 DB에 있는 것만" 보장된다. 이 흐름에서는 둘의 결과가 같다.

### 결과 전달(SSE)은 서버를 늘리면 깨진다

락과 작업 큐는 Redis에 있어 서버가 늘어도 동작한다. 하지만 결과 전달은 아니다.
- SSE 연결은 각 서버 메모리의 `ConcurrentHashMap`에 있다 ([SseEmitterService.java:22](../src/main/java/swyp12/team9/server/domain/sse/service/SseEmitterService.java:22)).
- 완료 알림은 컨슈머가 돈 서버 안에서 `ApplicationEventPublisher`로 전달된다.
- 서버가 두 대면, A 서버에 SSE로 붙은 사용자는 B 서버 컨슈머가 처리한 결과를 받지 못한다.

다중 서버로 가려면 Redis Pub/Sub 등으로 완료 이벤트를 모든 서버에 뿌리고, 각 서버가 자기 emitter에만 보내야 한다. 운영이 1대라 지금은 문제가 없다. "락이 공유 상태여야 한다"와 같은 원리로, **사용자 연결도 서버 메모리에 있으면 같은 한계를 갖는다**는 점을 보여 주는 사례다.

---

## 현재 세 장치의 역할 분담

| 장치 | 지키는 것 | 성격 | 뚫리면 |
|---|---|---|---|
| DB `uk_link_url_hash` UNIQUE + catch 후 재조회 ([LinkSaveService](../src/main/java/swyp12/team9/server/domain/link/service/LinkSaveService.java)) | Link 행 중복 | **정확성** | 뚫리지 않음 |
| `synchronized(urlHash)` ([UserLinkService.java:70](../src/main/java/swyp12/team9/server/domain/userlink/service/UserLinkService.java:70)) | placeholder 생성 시 UNIQUE 예외 빈도 줄이기 | 효율 | 예외 → catch로 수습 |
| Redis `processing_lock` | 스크래핑·AI 중복 실행 | **효율(비용)** | AI 호출 한 번 더, 데이터는 멀쩡 |
| 대기자 Set + drain Lua + SSE | 결과 전달 누락 | 사용자 경험 | 일부 사용자가 알림을 못 받음 (데이터는 정상) |

UserLink(사용자별 저장) 중복은 `findByUserIdAndLinkId` 사전 확인과 엔티티의 `uk_user_link (user_id, link_id)` ([UserLink.java:28](../src/main/java/swyp12/team9/server/domain/userlink/model/UserLink.java:28))가 맡는다. 다만 이 제약을 만드는 Flyway 마이그레이션은 없고 `ddl-auto: update`에만 맡겨져 있다. 테이블이 먼저 생긴 뒤 선언이 추가됐다면 운영 DB에 없을 수 있으니 `SHOW INDEX FROM user_links;`로 확인한 뒤 "DB가 보장한다"고 말할 것.

부록: `LinkStreamConsumer`의 `Semaphore(5)`, `Semaphore(8)`은 중복 방지가 아니라 **동시 실행 수 제한**이다. 같은 JVM 안의 자원 보호라 서버 내부 장치로 충분하다.

---

## 알려진 한계

### 락 값을 비교하지 않고 지운다

정석(값에 고유 토큰을 넣고 Lua로 비교 후 삭제)은 "내 락이 만료된 뒤 남이 잡은 락을 지우는" 상황을 막는다. Keepit에서 이 상황이 생기면:

1. 작업 1이 TTL(30분)을 넘겨 락 만료 — heartbeat가 있어 사실상 컨슈머가 멈춘 경우뿐
2. 새 요청이 락을 잡고 작업 2 발행
3. 작업 1이 끝나며 작업 2의 락과 대기자를 지움 → 대기자는 작업 1 결과로 알림을 받음 (같은 링크라 결과도 같음)
4. 작업 2는 그대로 돌고, 끝날 때 빈 대기자를 정리

결과: AI 호출이 한 번 더 나갈 수 있을 뿐 데이터와 알림은 정상이다. 효율용 락이라 감수했다. 정확성은 DB UNIQUE가 지킨다. 보강한다면 락 값에 `recordId`를 넣고 drain Lua에서 값이 같을 때만 지우면 된다.

### 2단계 서버 내부 락의 한계

```java
try { synchronized (lock) { ... } }
finally { URL_LOCKS.remove(urlHash); }
```
A가 끝나며 키를 지우는 순간 B가 같은 객체를 기다리는 중이었다면, 뒤이어 온 C는 **새 객체**를 받아 B와 동시에 들어간다. 지금은 안쪽이 수십 ms짜리 find-or-save이고 UNIQUE + catch가 받쳐 주니 문제가 되지 않지만, 서버 내부 락도 정확성을 보장하지는 않았다는 점을 보여 준다.

---

## 검증: 실제 Redis 동시성 테스트

[LinkStreamProducerConcurrencyTest](../src/test/java/swyp12/team9/server/domain/link/service/LinkStreamProducerConcurrencyTest.java)에서 실제 Redis에 `LinkStreamProducer`와 `LinkProcessStreamGateway`만 조립해(컨슈머 없음) `CountDownLatch`로 스레드를 동시에 출발시켰다. Redis가 없으면 테스트는 건너뛴다(`REDIS_TEST_PORT`, DB 15 사용).

| 시나리오 | 조건 | 결과 |
|---|---|---|
| 서로 다른 사용자 동시 저장 | 10명 × 20라운드 (요청 200건) | 매 라운드 Stream 발행 1건, 대기자 10명 |
| 같은 사용자 동시 저장 | 1명 × 10건 × 20라운드 | 매 라운드 발행 1건, 대기자 1명 |
| 완료 처리 | 위 상태에서 drain Lua 실행 | 대기자 10명 전원 반환, 락·대기자 키 삭제, 이후 재저장은 새로 1건 발행 |
| 완료 처리와 신규 요청 경합 | 작업 진행 중 drain 1개 + 신규 요청 10건 동시 출발 × 200라운드 (신규 2,000건) | 알림 대상 누락 0명 (4회 반복 모두 0) |
| 대조군: 조회와 삭제 분리 | 위와 같은 조건, `SMEMBERS`와 `DEL` 사이 1ms | 누락 5~13명 |

대조군의 1ms는 짧은 틈에서 드물게 생기는 경합을 재현하려고 일부러 넓힌 값이다. 운영 누락 수치가 아니라 "조회와 삭제 사이에 틈이 있으면 누락이 생긴다"는 확인이다.

경합 시나리오에서 대부분의 라운드는 drain 직후 도착한 요청이 새 작업을 발행했다. 락은 처리 중인 구간의 중복만 막으므로 의도된 동작이다.

---

## 한 문장 요약

> 처음엔 같은 URL의 조회~저장이 한 요청 안에서 끝나 서버 내부 락으로 충분했지만, 스크래핑·AI를 Redis Stream으로 분리하면서 락이 요청보다 오래 살고 다른 스레드가 풀어야 했기에, 작업 큐와 같은 Redis에 TTL 기반 분산 락을 두었다. 데이터 중복은 처음부터 끝까지 DB UNIQUE가 맡았다.
