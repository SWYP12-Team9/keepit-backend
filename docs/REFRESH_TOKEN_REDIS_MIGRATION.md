# refresh 토큰 저장소를 MySQL에서 Redis로 옮긴 이유

> keepit 프로젝트에서 JWT refresh 토큰 화이트리스트를 MySQL 테이블에서 Redis TTL 기반 세션 저장소로 전환한 기록입니다.
> 단순히 "Redis가 빠르니까"가 아니라, **왜 이 데이터에는 Redis가 맞는지**를 정리했습니다.

---

## 한 줄 요약

> **refresh 토큰은 "언제 사라져야 하는가"가 데이터의 본질인데, MySQL에는 그걸 표현할 방법이 없다.**

이 문장이 이번 작업의 전부입니다. 나머지는 이 명제에서 파생된 이야기입니다.

---

## 1. 배경: 왜 화이트리스트가 필요한가

JWT는 서버가 상태를 들고 있지 않아도 검증할 수 있다는 게 장점입니다. 그런데 그 장점이 그대로 단점이 됩니다.

**한 번 발급한 토큰은 만료 전까지 취소할 수 없습니다.**

로그아웃 버튼을 눌러도, 토큰이 탈취돼도, 서명이 유효한 이상 서버는 그 토큰을 받아들입니다. refresh 토큰은 수명이 길기 때문에(이 프로젝트는 28일) 이 문제가 특히 치명적입니다.

그래서 **서버가 발급한 refresh 토큰 목록을 따로 저장해두고, 목록에 없으면 거부**하는 방식을 씁니다. 이게 화이트리스트입니다.

- 로그아웃 → 목록에서 삭제 → 즉시 무효화
- 재발급(rotate) → 이전 토큰 삭제 + 새 토큰 등록 → 탈취된 옛 토큰 차단

즉 화이트리스트는 "JWT의 무상태성을 일부 포기하는 대신 취소 가능성을 얻는" 트레이드오프입니다.

---

## 2. Before: MySQL 테이블 방식

### 구조

```java
@Entity
@Table(name = "jwt_refresh")
public class JwtRefresh extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "refresh_token", nullable = false, length = 512)
    private String refreshToken;
}
```

```java
public interface RefreshRepository extends JpaRepository<JwtRefresh, Long> {
    Boolean existsByRefreshToken(String refreshToken);   // 화이트리스트 확인
    void deleteByRefreshToken(String refresh);           // 로그아웃
    void deleteByUsername(String username);              // 탈퇴
    void deleteByCreatedAtBefore(LocalDateTime cutoff);  // 만료 정리
}
```

MySQL에는 "이 행은 N일 뒤에 사라져라"는 개념이 없습니다. 그래서 배치로 흉내 냈습니다.

```java
@Scheduled(cron = "0 0 3 * * *")
public void refreshEntityTtlSchedule() {
    LocalDateTime cutoff = LocalDateTime.now().minusDays(8);
    refreshRepository.deleteByCreatedAtBefore(cutoff);
}
```

동작은 합니다. 문제는 그 다음입니다.

### 문제 1. 만료 기준이 두 곳에 따로 있었다 (실제 버그)

토큰의 실제 수명은 `JwtUtil`에 있습니다.

```java
// JwtUtil
refreshTokenExpiresIn = 604800L * 1000 * 4; // 7일 * 4 = 28일
```

정리 배치의 기준은 `ScheduleConfig`에 있습니다.

```java
// ScheduleConfig
LocalDateTime cutoff = LocalDateTime.now().minusDays(8);
```

**28일과 8일.** 두 숫자가 서로 다른 파일에서 각자 살고 있었습니다.

결과적으로 9일 전에 로그인한 사용자는 토큰이 19일이나 남았는데도 화이트리스트 행이 지워져 강제 로그아웃됐습니다. 의도한 세션 수명은 28일인데 실제로는 8일이었던 겁니다.

여기서 중요한 건 이게 **누가 실수해서 생긴 버그가 아니라는 점**입니다. 저장소에 만료 개념이 없으니 바깥에서 흉내 낼 수밖에 없었고, 흉내 내는 순간 "진짜 만료 시각"과 "정리 기준"이라는 두 개의 진실이 생긴 겁니다. 구조가 만든 버그입니다.

### 문제 2. 만료돼도 최대 하루 동안 살아있었다

배치는 하루에 한 번(새벽 3시) 돕니다. 오전 4시에 만료된 토큰은 다음 날 새벽 3시까지 23시간 동안 테이블에 남아 있습니다.

물론 JWT 서명 검증에서 `exp`를 보고 걸러내기 때문에 실제로 통과하지는 않습니다. 하지만 "자동 만료"라고 부르기엔 어색한 상태입니다.

### 문제 3. 인증 경로가 DB 커넥션을 물었다

```java
@Transactional(readOnly = true)
public Boolean existsRefresh(String refreshToken) {
    return refreshRepository.existsByRefreshToken(refreshToken);
}
```

토큰 하나 확인하는 데 트랜잭션을 열고 커넥션 풀에서 커넥션을 빌립니다. 게다가 `refresh_token` 컬럼에는 **인덱스가 선언돼 있지 않습니다**. 행이 쌓일수록 조회 비용이 선형으로 늘어나는 구조였습니다.

이 프로젝트는 예전에 커넥션 풀 고갈로 한 번 고생한 적이 있습니다. 인증은 사실상 모든 요청의 앞단이라 여기서 커넥션을 물지 않는 게 특히 중요합니다.

### 문제 4. "몇 개의 기기에서 로그인 중인가"를 알 수 없었다

`username`으로 행을 세면 되지 않냐고 할 수 있지만, 그러려면 또 인덱스 없는 조회를 해야 하고, 무엇보다 만료된 행과 살아있는 행이 섞여 있어 정확한 수를 알 수 없었습니다.

### 문제 5. 코드에 남은 우회 흔적

```java
removeRefresh(refreshToken);
refreshRepository.flush();   // 같은 트랜잭션 내부라 : 삭제 -> 생성 문제 해결
refreshRepository.save(newRefresh);
```

같은 트랜잭션에서 삭제와 삽입 순서가 꼬이는 걸 `flush()`로 강제 해결하고 있었습니다. JPA의 쓰기 지연 때문에 생긴 문제인데, 애초에 트랜잭션이 필요 없는 작업에 트랜잭션을 쓰다 보니 생긴 비용입니다.

---

## 3. 그래서 왜 Redis인가

### 이유 1. TTL이 일급 기능이다 (가장 중요)

refresh 토큰 레코드의 **가장 중요한 속성은 "정확히 시각 T에 사라져야 한다"** 입니다.

Redis는 이걸 그냥 기능으로 갖고 있습니다.

```java
stringRedisTemplate.opsForValue().set(sessionKey, username, ttl);
```

그리고 그 `ttl`을 토큰 자신의 `exp`에서 직접 계산합니다.

```java
public static Duration getRemainingValidity(String token) {
    Date expiration = Jwts.parser()...getPayload().getExpiration();
    long remainingMillis = expiration.getTime() - System.currentTimeMillis();
    return remainingMillis > 0 ? Duration.ofMillis(remainingMillis) : Duration.ZERO;
}
```

**만료 기준이 하나가 됩니다.** 토큰이 말하는 만료 시각이 곧 Redis 키의 만료 시각입니다. 앞서 본 "28일 vs 8일" 같은 불일치가 발생할 여지 자체가 사라집니다.

버그를 고친 게 아니라, **버그가 생길 수 있는 구조를 없앤 것**입니다.

### 이유 2. 접근 패턴이 순수한 키 조회다

이 데이터에 대한 모든 연산을 나열해보면 이렇습니다.

- 이 토큰이 존재하는가?
- 이 토큰을 지워라
- 이 유저의 토큰을 전부 지워라

조인도, 범위 검색도, 집계도, 리포팅도 없습니다. **관계형 DB가 제공하는 기능 중 쓰는 게 하나도 없습니다.** 그런데 비용(커넥션, 트랜잭션, 스캔)은 그대로 냈습니다.

Key-Value 저장소는 이 접근 패턴 그 자체를 위해 만들어진 도구입니다.

### 이유 3. 잃어도 되는 데이터다

이게 "Redis를 써도 되는가"에 대한 진짜 답입니다.

Redis는 기본적으로 메모리 기반이라 MySQL만큼 durable하지 않습니다. 보통은 이게 Redis를 못 쓰는 이유가 되는데, **세션 데이터에는 해당하지 않습니다.**

| | 유실되면 | 허용 가능? |
|---|---|---|
| 주문/결제 데이터 | 돈이 사라짐 | ❌ |
| 저장한 링크 | 사용자 자산 손실 | ❌ |
| **로그인 세션** | **다시 로그인하면 됨** | ✅ |

세션은 재생성 가능한 데이터입니다. 최악의 경우 사용자가 로그인을 한 번 더 하면 완전히 복구됩니다. 그래서 Redis의 약점을 감수할 수 있고, 감수하는 대가로 위의 장점들을 가져올 수 있습니다.

**"Redis가 빠르니까"가 아니라 "이 데이터는 Redis의 단점이 문제가 되지 않는 데이터니까"** 가 핵심입니다.

### 이유 4. 이미 있는 인프라였다

keepit은 이미 Redis를 세 곳에서 쓰고 있었습니다.

- Redis Stream — 스크래핑/AI 요약 비동기 처리 큐
- Redis 캐싱 — 카테고리 추천, 인기글 목록
- Redis INCR — 챗봇 요청 횟수 제한

새 인프라를 도입하는 게 아니라 이미 운영 중인 컴포넌트를 하나 더 활용하는 것이라, 운영 복잡도 증가가 거의 없었습니다. 만약 Redis가 없는 프로젝트였다면 "인증 하나 때문에 Redis를 새로 띄울 가치가 있나"를 훨씬 신중하게 따졌을 겁니다.

### 검토했던 대안들

| 대안 | 장점 | 왜 선택하지 않았나 |
|---|---|---|
| MySQL 유지 + 인덱스 추가 | 변경 최소, durable | 조회는 빨라지지만 **만료 불일치 구조는 그대로**. 커넥션도 계속 점유 |
| MySQL Event Scheduler | 배치를 DB 안으로 | 만료 기준이 여전히 토큰 밖에 있음. 운영/디버깅 난이도만 상승 |
| 화이트리스트 폐기 (순수 무상태 JWT) | 저장소 불필요, 가장 빠름 | **로그아웃 즉시 무효화가 불가능** — 요구사항 위반 |
| Redis 블랙리스트 (폐기 토큰만 저장) | 저장량 적음 | 세션 목록을 만들 수 없어 "기기별 관리"가 불가능. 아래 참고 |
| **Redis 화이트리스트** | TTL 일급, O(1), 세션 인덱스 | **선택** |

#### 블랙리스트 vs 화이트리스트

잠깐 짚고 갈 만한 선택지입니다.

- **블랙리스트**: 로그아웃된 토큰만 저장. 저장량이 훨씬 적음. 하지만 "유효한 세션 목록"이 없어서 활성 기기 조회나 전체 로그아웃을 구현할 수 없음. 또 저장소가 비면 **모든 토큰이 통과**하므로 유실에 취약(fail-open)
- **화이트리스트**: 발급된 토큰을 저장. 저장량이 더 많지만 세션 목록이 자연스럽게 생김. 저장소가 비면 **아무 토큰도 통과 못 함**(fail-close)

보안 관점에서 fail-close가 안전하고, 세션 단위 제어가 요구사항에 있었기 때문에 화이트리스트를 유지했습니다.

---

## 4. After: 설계

### 키 구조

| 키 | 타입 | 값 | TTL | 역할 |
|---|---|---|---|---|
| `auth:refresh_session:{tokenHash}` | String | username | 토큰 잔여 유효기간 | 세션 1건 |
| `auth:refresh_user:{username}` | Set | tokenHash 집합 | 최신 세션 기준 | 세션 인덱스 |

세션 키 하나만 있어도 화이트리스트 기능은 됩니다. **인덱스를 따로 둔 이유는 "세션 단위 제어"** 때문입니다. 유저 기준으로 세션을 모아볼 수 없으면 탈퇴 시 전체 폐기도, 활성 기기 수 조회도 불가능합니다.

### 왜 토큰을 해시해서 저장하는가

키에 토큰 원문을 그대로 쓰지 않고 SHA-256 해시를 씁니다.

```java
private String hash(String refreshToken) {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hashed = digest.digest(refreshToken.getBytes(StandardCharsets.UTF_8));
    return HexFormat.of().formatHex(hashed);
}
```

이유는 단순합니다. **Redis 덤프나 키 목록이 유출됐을 때 그 자체로 로그인 자격증명이 되면 안 되기 때문**입니다.

토큰 원문을 키로 쓰면 `KEYS auth:refresh_session:*` 한 번으로 살아있는 모든 refresh 토큰을 손에 넣을 수 있습니다. 해시로 저장하면 유출돼도 원문을 복원할 수 없어 그대로 재사용할 수 없습니다.

비밀번호를 평문으로 저장하지 않는 것과 같은 이유입니다. 부수적으로 512자 토큰이 64자 해시로 줄어 키 길이도 짧아집니다.

### 왜 전체 폐기에만 Lua를 썼는가

세션 1건 삭제는 평범합니다.

```java
public void remove(String username, String refreshToken) {
    String tokenHash = hash(refreshToken);
    stringRedisTemplate.delete(RefreshTokenRedisKeys.sessionKey(tokenHash));
    stringRedisTemplate.opsForSet().remove(RefreshTokenRedisKeys.userSessionsKey(username), tokenHash);
}
```

두 키를 지우지만 **호출 시점에 두 키를 모두 알고 있어서** 순서가 꼬여도 문제가 없습니다. 최악의 경우 인덱스에 고아 해시가 남는데, 조회할 때 걸러내면 됩니다.

문제는 **탈퇴/전체 로그아웃**입니다. 이건 "무엇을 지울지" 자체를 먼저 읽어와야 합니다.

```
1. SMEMBERS로 세션 해시 목록을 읽는다
2. 각 세션 키를 DEL 한다
3. 인덱스를 DEL 한다
```

1번과 3번 사이에 **다른 기기에서 로그인이 들어오면** 새 세션이 인덱스에 추가되는데, 3번이 인덱스를 통째로 지워버립니다. 결과적으로 **인덱스에는 없지만 살아있는 세션 키**가 남습니다. 탈퇴했는데 특정 토큰은 계속 동작하는 상황입니다.

그래서 이 구간만 Lua로 묶었습니다. Redis는 스크립트를 단일 명령처럼 원자적으로 실행합니다.

```lua
local hashes = redis.call('SMEMBERS', KEYS[1]);
for i = 1, #hashes do
  redis.call('DEL', ARGV[1] .. hashes[i]);
end
redis.call('DEL', KEYS[1]);
return #hashes;
```

**모든 걸 Lua로 감싸는 게 아니라, 읽고-쓰는 사이에 끼어들 여지가 있는 곳만** 원자적으로 만드는 게 포인트입니다.

---

## 5. 작업 중 발견한 버그: 같은 토큰이 두 번 발급되고 있었다

테스트를 짜다가 이상한 실패를 만났습니다.

```java
// 재발급했는데 이전 토큰이 여전히 살아있다?
assertThat(jwtService.existsRefresh(oldToken)).isFalse();  // 실패
```

원인은 JWT 스펙에 있었습니다. **`iat`와 `exp`는 초 단위(NumericDate)로 직렬화됩니다.**

```java
// Before - 고유 식별자가 없다
return Jwts.builder()
        .claim("sub", username)
        .claim("userId", userId)
        .claim("role", role)
        .claim("type", type)
        .issuedAt(new Date(now))        // 초 단위로 저장됨
        .expiration(new Date(now + expiry))
        .signWith(secretKey)
        .compact();
```

같은 유저에게 같은 초에 토큰을 두 번 발급하면, 모든 클레임이 동일하므로 **바이트 단위로 완전히 똑같은 문자열**이 나옵니다.

MySQL 방식에서는 행이 두 개 쌓일 뿐이라 티가 안 났습니다. 하지만 **토큰을 키로 쓰는 순간** 이야기가 달라집니다.

- 두 기기에서 1초 안에 로그인 → 세션 키가 하나로 합쳐짐
- 한 기기에서 로그아웃 → 다른 기기도 함께 로그아웃

세션 단위 제어를 하겠다면서 세션이 구분되지 않는 상태였던 겁니다.

```java
// After - jti(JWT ID)로 매 토큰을 고유하게
.id(UUID.randomUUID().toString())
```

저장소를 바꾸지 않았다면 계속 모르고 지나갔을 버그입니다. **구조를 바꾸니까 원래 있던 문제가 드러난** 사례라 개인적으로 가장 기억에 남습니다.

---

## 6. Before / After 비교

| 항목 | Before (MySQL) | After (Redis) |
|---|---|---|
| 저장소 | `jwt_refresh` 테이블 | `auth:refresh_session:{hash}` |
| 만료 처리 | 새벽 3시 배치 | TTL (토큰 `exp`와 동일) |
| 만료 기준 | 코드 두 곳에 분산 (28일 / 8일) | 토큰 자신이 유일한 기준 |
| 만료 지연 | 최대 약 24시간 | 없음 |
| 실질 세션 수명 | 약 8일 (의도는 28일) | 28일 (의도대로) |
| 토큰 조회 | 인덱스 없는 컬럼 조회 | `EXISTS` — O(1) |
| 커넥션 | 검증마다 트랜잭션 + 커넥션 | 커넥션 풀 미사용 |
| 저장 형태 | 토큰 원문 512자 | SHA-256 해시 64자 |
| 세션 목록 | 파악 불가 | Set 인덱스로 조회 |
| 전체 폐기 | 단일 DELETE | Lua 원자적 처리 |
| 토큰 고유성 | 같은 초 발급 시 충돌 | `jti`로 항상 고유 |
| 코드 | 회전 로직 중복 + `flush()` 우회 | `rotate()` 단일화, 트랜잭션 제거 |

---

## 7. 장단점 정리

### 얻은 것

| | 내용 |
|---|---|
| **만료 정합성** | 만료 기준이 토큰 하나로 통일. 불일치 버그가 구조적으로 불가능해짐 |
| **즉시성** | 만료 시각에 정확히 사라짐. 정리 배치 불필요 |
| **인증 경로 경량화** | DB 커넥션/트랜잭션 제거, O(1) 조회 |
| **세션 단위 제어** | 활성 기기 수 조회, 전체 기기 로그아웃이 실제로 동작 |
| **저장 안전성** | 해시 저장으로 Redis 유출 시 토큰 재사용 차단 |
| **동시성 안전성** | 탈퇴 처리를 우회하는 레이스 컨디션 차단 |
| **코드 단순화** | 중복 회전 로직 통합, `flush()` 우회 제거 |
| **부수 효과** | `jti` 누락 버그 발견 및 수정 |

### 잃은 것 / 감수한 것

솔직하게 적자면 공짜는 아닙니다.

| | 내용 | 완화 방법 |
|---|---|---|
| **영속성 후퇴** | Redis 재시작 시 세션 소실 가능. MySQL은 durable했음 | AOF/RDB 영속화 설정. 최악의 경우도 재로그인으로 복구 |
| **의존성 집중** | Redis 장애 시 토큰 재발급 전면 중단 | 이미 Stream/캐싱도 Redis에 의존 중이라 장애 영향 범위가 크게 늘지는 않음 |
| **운영 가시성 저하** | SQL로 훑어보던 걸 못 함. `KEYS`는 운영에서 위험해 `SCAN` 필요 | 세션 수 조회 API로 대체 |
| **메모리 비용** | 디스크가 아닌 메모리를 씀 | 세션당 수십 바이트 수준이라 영향 미미 |
| **배포 시 전체 로그아웃** | 기존 세션이 MySQL에만 있어 마이그레이션 경로 없음 | 트래픽 적은 시간대 배포. 아래 참고 |

### 배포 시 주의사항

기존 refresh 토큰은 MySQL에만 있고 Redis 화이트리스트는 비어 있습니다. 따라서 **배포 직후 모든 재발급 요청이 거부**되고, 사용자는 access 토큰 만료 시점에 재로그인해야 합니다.

굳이 무중단으로 가려면 "Redis에 없으면 MySQL을 확인하고 있으면 Redis로 옮긴다"는 이중 조회 구간을 한동안 두는 방법이 있습니다. 다만 이번에는 서비스 규모와 재로그인의 부담을 고려해 단순 전환을 선택했습니다.

---

## 8. 정리

이번 작업에서 배운 건 **저장소 선택이 성능 문제이기 이전에 모델링 문제**라는 점입니다.

"MySQL이 느려서 Redis로 바꿨다"가 아닙니다. refresh 토큰이라는 데이터의 성격 —

- 정해진 시각에 사라져야 하고
- 키로만 접근하며
- 잃어버려도 재생성 가능한 —

이 성격을 그대로 표현할 수 있는 도구가 Redis였고, MySQL은 그중 첫 번째를 표현할 수단이 없어서 배치라는 우회로를 만들었고, 그 우회로에서 버그가 나왔습니다.

**도구가 도메인 개념을 직접 표현하지 못하면, 그 간극을 메우는 코드에서 버그가 난다.** 이번 작업의 결론입니다.

---

### 참고

- 관련 PR: [refactor: refresh 토큰 저장소를 Redis TTL 기반으로 전환 #134](https://github.com/SWYP12-Team9/keepit-backend/pull/134)
- 주요 파일
  - `domain/auth/jwt/infrastructure/RefreshTokenStore.java`
  - `domain/auth/jwt/infrastructure/RefreshTokenRedisKeys.java`
  - `domain/auth/jwt/service/JwtService.java`
  - `global/util/JwtUtil.java`
