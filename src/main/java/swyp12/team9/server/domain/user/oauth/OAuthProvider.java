package swyp12.team9.server.domain.user.oauth;

import org.springframework.security.oauth2.core.user.OAuth2User;
import swyp12.team9.server.domain.user.model.SocialProvider;

/**
 * OAuth2 프로바이더별 사용자 정보 추출 인터페이스
 */
public interface OAuthProvider {

    /**
     * 지원하는 소셜 프로바이더 타입 반환
     */
    SocialProvider getProviderType();

    /**
     * OAuth2User에서 사용자 정보 추출
     */
    OAuthUserInfo extractUserInfo(OAuth2User oAuth2User);
}
