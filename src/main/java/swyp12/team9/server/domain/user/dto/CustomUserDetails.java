package swyp12.team9.server.domain.user.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import swyp12.team9.server.domain.user.model.User;
import swyp12.team9.server.global.annotation.UserPrincipal;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 자체 로그인 사용자 정보를 담는 UserDetails 구현체
 * - UserDetails: 자체 로그인용
 * - userId를 포함하여 매 요청마다 DB 조회 불필요
 * - Security Context에 저장되어 인증된 사용자 정보를 제공
 */
// 자체 로그인용
@Getter
@Builder
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails, UserPrincipal {

    private final Long userId;
    private final String username;
    private final String password;
    private final String email;
    private final String roleType;
    private final Boolean isLock;
    private final Boolean isSocial;
    private final Collection<? extends GrantedAuthority> authorities;
    private final Map<String, Object> attributes;


    // User 엔티티로부터 생성
    public static CustomUserDetails from(User user) {
        return CustomUserDetails.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .password(user.getPassword())
                .email(user.getEmail())
                .roleType(user.getRoleType().name())
                .isLock(user.getIsLock())
                .isSocial(user.getIsSocial())
                .build();
    }

    // UserDetails 구현
    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        //return authorities;
        return List.of(new SimpleGrantedAuthority("ROLE_" + roleType));
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    // UserPrincipal 인터페이스 구현
    @Override
    public Long getUserId() { return userId; }

    @Override
    public String getEmail() { return email; }

    @Override
    public String getRoleType() { return roleType; }

    @Override
    public Boolean getIsSocial() { return isSocial; }
}
