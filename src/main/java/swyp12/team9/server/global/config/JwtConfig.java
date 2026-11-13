package swyp12.team9.server.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import swyp12.team9.server.global.util.JwtUtil;

@Configuration
public class JwtConfig {

    // jwt.secret이 없으면 플레이스홀더를 풀 수 없어 애플리케이션이 시작되지 않는다(배포 시 즉시 확인 가능).
    public JwtConfig(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expires-in-ms}") long accessExpiresInMs,
            @Value("${jwt.refresh-token-expires-in-ms}") long refreshExpiresInMs
    ) {
        JwtUtil.configure(secret, accessExpiresInMs, refreshExpiresInMs);
    }
}
