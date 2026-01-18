package swyp12.team9.server.global.security;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import swyp12.team9.server.global.annotation.UserPrincipal;

import java.util.Collection;
import java.util.Map;

/**
 * 소셜 로그인 사용자 정보를 담는 OAuth2User 구현체
 * Security Context에 저장되어 인증된 사용자 정보를 제공
 */
// 소셜 로그인용
@Getter
@Builder
@RequiredArgsConstructor
public class CustomOAuth2User implements OAuth2User, UserPrincipal {
    private final Map<String, Object> attributes;
    private final Collection<? extends GrantedAuthority> authorities;
    private final Long userId;
    private final String username;
    private final String email;
    private final String roleType;

    public CustomOAuth2User(Map<String, Object> attributes,
                            Collection<? extends GrantedAuthority> authorities,
                            String username,
                            Long userId,
                            String email,
                            String roleType) {
        this.attributes = attributes;
        this.authorities = authorities;
        this.username = username;
        this.userId = userId;
        this.email = email;
        this.roleType = roleType;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getName() {
        return username;
    }

    // UserPrincipal 인터페이스 구현
    @Override
    public Long getUserId() { return userId; }

    @Override
    public String getUsername() { return username; }

    @Override
    public String getEmail() { return email; }

    @Override
    public String getRoleType() { return roleType; }

    @Override
    public Boolean getIsSocial() { return true; }
}
