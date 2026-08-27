package swyp12.team9.server.domain.auth.jwt.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

/**
 * Redis TTL 기반 refresh 토큰 화이트리스트 저장소
 *
 * <p>기존에는 refresh 토큰을 MySQL 테이블(jwt_refresh)에 저장하고, 만료된 행은 매일 새벽 배치로 지웠다.
 * 이 방식은 (1) 토큰 만료와 실제 삭제 시점 사이에 최대 하루의 공백이 생기고,
 * (2) 인증 경로마다 DB 커넥션을 점유하며, (3) 세션 단위 조회/폐기 수단이 없다는 문제가 있었다.
 *
 * <p>키 구조
 * <ul>
 *   <li>{@code auth:refresh_session:{tokenHash}} - 세션 1건. value는 username, TTL은 토큰 만료까지의 잔여 시간</li>
 *   <li>{@code auth:refresh_user:{username}} - 유저가 보유한 세션 해시 집합 (세션 단위 제어용 인덱스)</li>
 * </ul>
 *
 * <p>토큰 원문 대신 SHA-256 해시를 키로 사용한다. Redis 덤프나 키 조회 로그가 유출돼도
 * 토큰 자체를 복원할 수 없어 그대로 재사용당하는 것을 막는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenStore {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 세션 저장. TTL을 토큰의 잔여 유효기간으로 맞춰 별도 삭제 배치 없이 Redis가 자동 만료시킨다.
     */
    public void save(String username, String refreshToken, Duration ttl) {
        // 이미 만료된 토큰을 저장하면 TTL이 없는 영구 키가 남으므로 저장하지 않는다
        if (ttl.isZero() || ttl.isNegative()) {
            log.warn("[refresh 세션] 만료된 토큰이라 저장하지 않음 - username: {}", username);
            return;
        }

        String tokenHash = hash(refreshToken);
        String userSessionsKey = RefreshTokenRedisKeys.userSessionsKey(username);

        stringRedisTemplate.opsForValue().set(RefreshTokenRedisKeys.sessionKey(tokenHash), username, ttl);
        stringRedisTemplate.opsForSet().add(userSessionsKey, tokenHash);

        // 모든 refresh 토큰의 수명이 동일하므로 가장 최근에 발급된 세션이 항상 가장 늦게 만료된다.
        // 따라서 인덱스 TTL을 방금 저장한 세션 기준으로 연장하면 살아있는 세션이 인덱스보다 오래 남는 일은 없다.
        stringRedisTemplate.expire(userSessionsKey, ttl);
    }

    /**
     * 화이트리스트 존재 여부. 서명이 유효해도 로그아웃/폐기된 토큰이면 false를 반환한다.
     */
    public boolean exists(String refreshToken) {
        return Boolean.TRUE.equals(
                stringRedisTemplate.hasKey(RefreshTokenRedisKeys.sessionKey(hash(refreshToken)))
        );
    }

    /**
     * 세션 1건 즉시 무효화. 로그아웃과 rotate 시 직전 토큰을 폐기하는 데 사용한다.
     * 두 키 모두 호출 시점에 알 수 있어 Lua 없이도 안전하며, 중간에 실패해도 인덱스에 고아 해시만 남는다.
     */
    public void remove(String username, String refreshToken) {
        String tokenHash = hash(refreshToken);

        stringRedisTemplate.delete(RefreshTokenRedisKeys.sessionKey(tokenHash));
        stringRedisTemplate.opsForSet().remove(RefreshTokenRedisKeys.userSessionsKey(username), tokenHash);
    }

    /**
     * 특정 유저의 모든 세션 폐기(탈퇴, 전체 기기 로그아웃).
     *
     * <p>SMEMBERS로 목록을 읽고 개별 DEL 하는 사이에 다른 기기에서 로그인이 발생하면
     * 인덱스만 지워지고 새 세션 키는 살아남아 폐기를 우회할 수 있다.
     * 조회와 삭제를 Lua 스크립트 하나로 묶어 이 구간을 원자적으로 처리한다.
     *
     * @return 폐기된 세션 수
     */
    public long removeAllByUsername(String username) {
        String script =
                "local hashes = redis.call('SMEMBERS', KEYS[1]); " +
                "for i = 1, #hashes do " +
                "  redis.call('DEL', ARGV[1] .. hashes[i]); " +
                "end " +
                "redis.call('DEL', KEYS[1]); " +
                "return #hashes;";

        RedisScript<Long> revokeAllScript = new DefaultRedisScript<>(script, Long.class);
        Long revoked = stringRedisTemplate.execute(
                revokeAllScript,
                List.of(RefreshTokenRedisKeys.userSessionsKey(username)),
                RefreshTokenRedisKeys.sessionKey("")
        );

        return revoked == null ? 0L : revoked;
    }

    /**
     * 유저의 활성 세션 수. 인덱스에는 TTL로 이미 사라진 세션의 해시가 남아 있을 수 있어
     * 실제 세션 키가 살아있는 것만 센다.
     */
    public long countActiveSessions(String username) {
        Set<String> tokenHashes =
                stringRedisTemplate.opsForSet().members(RefreshTokenRedisKeys.userSessionsKey(username));

        if (tokenHashes == null || tokenHashes.isEmpty()) {
            return 0L;
        }

        return tokenHashes.stream()
                .map(RefreshTokenRedisKeys::sessionKey)
                .filter(sessionKey -> Boolean.TRUE.equals(stringRedisTemplate.hasKey(sessionKey)))
                .count();
    }

    private String hash(String refreshToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(refreshToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }
}
