package swyp12.team9.server.domain.user.oauth;

import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import swyp12.team9.server.domain.user.model.SocialProvider;

import java.util.Map;

/**
 * 카카오 OAuth2 프로바이더
 */
@Component
public class KakaoOAuthProvider implements OAuthProvider {

    @Override
    public SocialProvider getProviderType() {
        return SocialProvider.KAKAO;
    }

    @Override
    public OAuthUserInfo extractUserInfo(OAuth2User oAuth2User) {
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // 카카오 ID
        String id = attributes.get("id").toString();
        String username = getProviderType().name() + "_" + id;

        // kakao_account 정보 추출
        @SuppressWarnings("unchecked")
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");

        // profile 정보 추출
        @SuppressWarnings("unchecked")
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

        // 닉네임 추출 (없으면 "카카오유저" + ID)
        String nickname = "카카오유저" + id;
        if (profile != null && profile.containsKey("nickname") && profile.get("nickname") != null) {
            nickname = profile.get("nickname").toString();
        }

        return OAuthUserInfo.builder()
                .username(username)
                .email(null)  // 카카오는 이메일 제공 불가
                .nickname(nickname)
                .providerType(getProviderType())
                .attributes(attributes)
                .build();
    }
}
