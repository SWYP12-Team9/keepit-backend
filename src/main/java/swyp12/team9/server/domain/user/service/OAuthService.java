package swyp12.team9.server.domain.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swyp12.team9.server.api.user.dto.request.UserRequest;
import swyp12.team9.server.global.security.CustomOAuth2User;
import swyp12.team9.server.domain.user.model.SocialProvider;
import swyp12.team9.server.domain.user.model.User;
import swyp12.team9.server.domain.user.model.UserRole;
import swyp12.team9.server.domain.user.oauth.OAuthProvider;
import swyp12.team9.server.domain.user.oauth.OAuthUserInfo;
import swyp12.team9.server.domain.user.repository.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 소셜 로그인 관련 서비스 (네이버, 구글, 카카오)
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class OAuthService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final Map<SocialProvider, OAuthProvider> providerMap;

    public OAuthService(UserRepository userRepository, List<OAuthProvider> providers) {
        this.userRepository = userRepository;
        this.providerMap = providers.stream()
                .collect(Collectors.toMap(OAuthProvider::getProviderType, Function.identity()));
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId().toUpperCase();
        SocialProvider providerType;

        try {
            providerType = SocialProvider.valueOf(registrationId);
        } catch (IllegalArgumentException e) {
            throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인입니다: " + registrationId);
        }

        OAuthProvider provider = providerMap.get(providerType);
        if (provider == null) {
            throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인입니다: " + registrationId);
        }

        OAuthUserInfo userInfo = provider.extractUserInfo(oAuth2User);

        User user = findOrCreateUser(userInfo);

        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRoleType().name())
        );

        return new CustomOAuth2User(
                userInfo.getAttributes(),
                authorities,
                userInfo.getUsername(),
                user.getId(),
                user.getEmail(),
                user.getRoleType().name()
        );
    }

    // 메서드
    private User findOrCreateUser(OAuthUserInfo userInfo) {

        // 1. username으로 소셜 계정 찾기
        Optional<User> existingUser = userRepository.findByUsernameAndIsSocial(userInfo.getUsername(), true);

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            updateUserInfo(user, userInfo);
            return userRepository.save(user);
        }

        // 2. 이메일이 있는 경우에만 중복 체크
        if (userInfo.getEmail() != null && !userInfo.getEmail().isBlank()) {
            Optional<User> userWithSameEmail = userRepository.findByEmail(userInfo.getEmail());

            if (userWithSameEmail.isPresent()) {
                User existingUserByEmail = userWithSameEmail.get();

                // 자체 회원가입 계정이 있는 경우
                if (!existingUserByEmail.getIsSocial()) {
                    throw new OAuth2AuthenticationException(
                            "이미 가입된 이메일입니다. 자체 로그인을 사용해주세요."
                    );
                }

                // 다른 소셜 로그인으로 이미 가입된 경우
                throw new OAuth2AuthenticationException(
                        "이미 " + existingUserByEmail.getSocialProvider() +
                                " 계정으로 가입된 이메일입니다."
                );
            }
        }

        return createNewUser(userInfo);
    }

    private void updateUserInfo(User user, OAuthUserInfo userInfo) {
        UserRequest request = new UserRequest();
        request.setNickname(userInfo.getNickname());
        request.setEmail(userInfo.getEmail());
        user.updateUser(request);
    }

    private User createNewUser(OAuthUserInfo userInfo) {
        User newUser = User.builder()
                .username(userInfo.getUsername())
                .password("")
                .isLock(false)
                .isSocial(true)
                .socialProvider(userInfo.getProviderType())
                .roleType(UserRole.USER)
                .nickname(userInfo.getNickname())
                .email(userInfo.getEmail())
                .name(userInfo.getNickname())
                .build();

        return userRepository.save(newUser);
    }
}
