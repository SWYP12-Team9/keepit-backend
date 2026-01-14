package swyp12.team9.server.domain.user.oauth;

import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import swyp12.team9.server.domain.user.model.SocialProviderType;

import java.util.Map;

/**
 * 카카오 OAuth2 프로바이더
 */
@Component
public class KakaoOAuthProvider implements OAuthProvider {

    @Override
    public SocialProviderType getProviderType() {
        return SocialProviderType.KAKAO;
    }

    @Override
    public OAuthUserInfo extractUserInfo(OAuth2User oAuth2User) {
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String id = attributes.get("id").toString();
        String username = getProviderType().name() + "_" + id;

        @SuppressWarnings("unchecked")
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        @SuppressWarnings("unchecked")
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

        String email = kakaoAccount.get("email") != null ? kakaoAccount.get("email").toString() : "";
        String nickname = profile.get("nickname") != null ? profile.get("nickname").toString() : "";

        return OAuthUserInfo.builder()
                .username(username)
                .email(email)
                .nickname(nickname)
                .providerType(getProviderType())
                .attributes(attributes)
                .build();
    }
}
