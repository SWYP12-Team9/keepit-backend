package swyp12.team9.server.domain.user.oauth;

import lombok.Builder;
import lombok.Getter;
import swyp12.team9.server.domain.user.model.SocialProviderType;

import java.util.Map;

/**
 * OAuth 프로바이더로부터 추출된 사용자 정보
 */
@Getter
@Builder
public class OAuthUserInfo {

    private final String username;
    private final String email;
    private final String nickname;
    private final SocialProviderType providerType;
    private final Map<String, Object> attributes;
}
