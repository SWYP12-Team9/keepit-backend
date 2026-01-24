package swyp12.team9.server.domain.user.oauth;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
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
        Object responseObj = oAuth2User.getAttributes().get("response");
        if (responseObj == null) {
            throw new OAuth2AuthenticationException("네이버 API 응답에서 'response' 필드를 찾을 수 없습니다.");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = (Map<String, Object>) responseObj;

        Object idObj = attributes.get("id");
        Object emailObj = attributes.get("email");
        Object nicknameObj = attributes.get("nickname");

        if (idObj == null) {
            throw new OAuth2AuthenticationException("네이버 사용자 ID를 가져올 수 없습니다.");
        }

        String id = idObj.toString();
        String username = getProviderType().name() + "_" + id;
        String email = emailObj != null ? emailObj.toString() : null;
        String nickname = nicknameObj != null ? nicknameObj.toString() : "네이버사용자";

        return OAuthUserInfo.builder()
                .username(username)
                .email(email)
                .nickname(nickname)
                .providerType(getProviderType())
                .attributes(attributes)
                .build();
    }
}
