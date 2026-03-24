# REQUIRES_NEW가 필요한 이유

## 배경

`UserLinkService`에서 `ConcurrentHashMap` + `synchronized`로 URL 단위 메모리 락을 잡고 있지만,
`LinkSaveService.findOrSaveLink()`에 `REQUIRES_NEW`가 없으면 동시 요청 시 중복 생성이 발생할 수 있다.

## 핵심: 락 해제 시점 vs 커밋 시점

`synchronized` 락은 **코드 블록이 끝나면** 해제되고,
`@Transactional` 커밋은 **메서드가 완전히 리턴된 후** Spring 프록시에서 실행된다.
이 두 시점이 다르기 때문에 문제가 발생한다.

## REQUIRES_NEW 없이 (REQUIRED) — 문제 발생

`findOrSaveLink()`이 외부 트랜잭션 A에 참여하는 경우:

```
UserLinkService.createUserLink() ─── 트랜잭션 A ──────────────────────────
                                                                        COMMIT

  스레드 A: synchronized {                           } ← lock 해제
              linkService.getOrCreateLink()
                └─ linkSaveService.findOrSaveLink()
                     find → null → save              ← 트랜잭션 A 안이라 커밋 안 됨

  스레드 B:                  synchronized {                          }
                               linkService.getOrCreateLink()
                                 └─ linkSaveService.findOrSaveLink()
                                      find → null ← A가 아직 커밋 안 함!
                                      save → 중복 생성!
```

`save()`가 실행되어도 트랜잭션 A는 `createUserLink()` 끝에서야 커밋된다.
락은 그 전에 해제되므로, 스레드 B는 **커밋되지 않은 데이터를 조회**한다.

## REQUIRES_NEW 사용 — 문제 해결

`findOrSaveLink()`이 독립 트랜잭션 B에서 실행되는 경우:

```
UserLinkService.createUserLink() ─── 트랜잭션 A ──────────────────────────

  스레드 A: synchronized {                           } ← lock 해제
              linkService.getOrCreateLink()
                └─ linkSaveService.findOrSaveLink() ─ 트랜잭션 B ─
                     find → null → save → COMMIT ✅  ← 메서드 리턴 전에 커밋 완료

  스레드 B:                  synchronized {                          }
                               linkService.getOrCreateLink()
                                 └─ linkSaveService.findOrSaveLink() ─ 트랜잭션 C ─
                                      find → 있음 ✅  ← B의 커밋 데이터가 보임
```

`REQUIRES_NEW` 트랜잭션은 메서드 리턴 시점에 커밋이 완료된다.
락은 그 **이후**에 해제되므로, 스레드 B는 **커밋된 데이터를 확실히 조회**할 수 있다.

## 비교 표

| | 락 해제 시점 | 커밋 시점 | 결과 |
|---|---|---|---|
| **REQUIRED** | `synchronized` 블록 끝 | `createUserLink()` 끝 | 락 해제 < 커밋 → 데이터 안 보임 |
| **REQUIRES_NEW** | `synchronized` 블록 끝 | `findOrSaveLink()` 리턴 | 커밋 < 락 해제 → 데이터 보임 |

## 결론

- `synchronized`는 **코드 실행 순서**를 보장한다.
- `REQUIRES_NEW`는 **DB 가시성**을 보장한다.
- 둘 다 있어야 동시 요청에서 안전하다.

## 참고

- `LinkSaveService.findOrSaveLink()`에만 `REQUIRES_NEW`를 적용하여, 커넥션 점유 시간을 수십ms로 최소화했다.
- 외부 호출(스크래핑, AI 요약)은 `LinkService`에서 트랜잭션 밖으로 분리하여 커넥션을 사용하지 않는다.
- 관련 문서: [LINK_DUPLICATE_FIX.md](LINK_DUPLICATE_FIX.md)
