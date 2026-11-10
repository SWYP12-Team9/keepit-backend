package swyp12.team9.server.global.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

public final class JwtUtil {

    private static volatile SecretKey secretKey;
    private static volatile long accessTokenExpiresIn;
    private static volatile long refreshTokenExpiresIn;

    private JwtUtil() {
    }

    // 서명 키와 만료 시간은 설정(jwt.*)에서 주입한다. 애플리케이션 시작 시 JwtConfig가 한 번 호출한다.
    public static void configure(String secret, long accessExpiresInMs, long refreshExpiresInMs) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("jwt.secret(JWT_SECRET)은 32바이트 이상이어야 합니다.");
        }
        secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), Jwts.SIG.HS256.key().build().getAlgorithm());
        accessTokenExpiresIn = accessExpiresInMs;
        refreshTokenExpiresIn = refreshExpiresInMs;
    }

    // JWT 클레임 username 파싱
    public static String getUsername(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("sub", String.class);
    }

    // JWT 클레임 role 파싱
    public static String getRole(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("role", String.class);
    }

    // JWT 클레임 userId 파싱
    public static Long getUserId(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("userId", Long.class);
    }

    // 토큰 만료까지 남은 시간 (Redis 세션 저장소의 TTL로 사용)
    // 이미 만료된 토큰은 Duration.ZERO를 반환한다.
    public static Duration getRemainingValidity(String token) {
        Date expiration = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();

        long remainingMillis = expiration.getTime() - System.currentTimeMillis();
        return remainingMillis > 0 ? Duration.ofMillis(remainingMillis) : Duration.ZERO;
    }

    // JWT 유효 여부 (위조, 시간, Access/Refresh 여부)
    public static Boolean isValid(String token, Boolean isAccess) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String type = claims.get("type", String.class);
            if (type == null) return false;

            if (isAccess && !type.equals("access")) return false;
            if (!isAccess && !type.equals("refresh")) return false;

            return true;

        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // JWT(Access/Refresh) 생성
    public static String createJWT(Long userId, String username, String role, Boolean isAccess) {

        long now = System.currentTimeMillis();
        long expiry = isAccess ? accessTokenExpiresIn : refreshTokenExpiresIn;
        String type = isAccess ? "access" : "refresh";

        return Jwts.builder()
                // jti: 토큰마다 고유한 식별자.
                // iat/exp는 초 단위로 직렬화되기 때문에 jti가 없으면 같은 유저가 1초 안에 두 번 로그인할 때
                // 완전히 동일한 토큰이 발급되어 서로 다른 기기의 세션이 하나로 합쳐진다.
                .id(UUID.randomUUID().toString())
                .claim("sub", username)
                .claim("userId", userId)
                .claim("role", role)
                .claim("type", type)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expiry))
                .signWith(secretKey)
                .compact();
    }

}