package swyp12.team9.server.domain.user.oauth;

import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import swyp12.team9.server.domain.user.model.SocialProvider;

import java.util.Map;

/**
 * 네이버 OAuth2 프로바이더
 */
@Component
public class NaverOAuthProvider implements OAuthProvider {

    @Override
    public SocialProvider getProviderType() {
        return SocialProvider.NAVER;
    }

    @Override
    public OAuthUserInfo extractUserInfo(OAuth2User oAuth2User) {
        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = (Map<String, Object>) oAuth2User.getAttributes().get("response");

        String id = attributes.get("id").toString();
        String username = getProviderType().name() + "_" + id;
        String email = attributes.get("email").toString();
        String nickname = attributes.get("nickname").toString();

        return OAuthUserInfo.builder()
                .username(username)
                .email(email)
                .nickname(nickname)
                .providerType(getProviderType())
                .attributes(attributes)
                .build();
    }
}
