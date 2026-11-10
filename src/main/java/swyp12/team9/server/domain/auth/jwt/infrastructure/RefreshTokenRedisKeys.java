package swyp12.team9.server.domain.auth.jwt.infrastructure;

import lombok.experimental.UtilityClass;

@UtilityClass
public class RefreshTokenRedisKeys {

    // 세션 1건(발급된 refresh 토큰 1개)을 가리키는 키. value는 username, TTL은 토큰 만료 시각까지
    public static String sessionKey(String tokenHash) {
        return "auth:refresh_session:" + tokenHash;
    }

    // 한 유저가 보유한 세션 해시 목록. 탈퇴/전체 로그아웃처럼 세션을 한 번에 정리할 때 사용
    public static String userSessionsKey(String username) {
        return "auth:refresh_user:" + username;
    }
}
