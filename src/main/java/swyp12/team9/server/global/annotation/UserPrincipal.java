package swyp12.team9.server.global.annotation;

/**
 * 자체 로그인(CustomUserDetails)과 소셜 로그인(CustomOAuth2User)을 통합하는 인터페이스
 */
public interface UserPrincipal {
    Long getUserId();
    String getUsername();
    String getEmail();
    String getRoleType();
    Boolean getIsSocial();
}