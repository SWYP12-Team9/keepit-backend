package swyp12.team9.server.domain.user.oauth;

import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import swyp12.team9.server.domain.user.model.SocialProvider;

import java.util.Map;

/**
 * 구글 OAuth2 프로바이더
 */
@Component
public class GoogleOAuthProvider implements OAuthProvider {

    @Override
    public SocialProvider getProviderType() {
        return SocialProvider.GOOGLE;
    }

    @Override
    public OAuthUserInfo extractUserInfo(OAuth2User oAuth2User) {
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String sub = attributes.get("sub").toString();
        String username = getProviderType().name() + "_" + sub;
        String email = attributes.get("email").toString();
        String nickname = attributes.get("name").toString();

        return OAuthUserInfo.builder()
                .username(username)
                .email(email)
                .nickname(nickname)
                .providerType(getProviderType())
                .attributes(attributes)
                .build();
    }
}
