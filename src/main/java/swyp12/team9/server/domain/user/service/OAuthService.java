package swyp12.team9.server.domain.user.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swyp12.team9.server.domain.reference.model.Reference;
import swyp12.team9.server.domain.reference.repository.ReferenceRepository;
import swyp12.team9.server.domain.user.model.SocialProvider;
import swyp12.team9.server.domain.user.model.User;
import swyp12.team9.server.domain.user.model.UserRole;
import swyp12.team9.server.domain.user.model.UserSocialLink;
import swyp12.team9.server.domain.user.model.UserStatus;
import swyp12.team9.server.domain.user.oauth.OAuthProvider;
import swyp12.team9.server.domain.user.oauth.OAuthUserInfo;
import swyp12.team9.server.domain.user.repository.UserRepository;
import swyp12.team9.server.domain.user.repository.UserSocialLinkRepository;
import swyp12.team9.server.global.security.CustomOAuth2User;

/**
 * 소셜 로그인 관련 서비스 (네이버, 구글, 카카오) - 동일 이메일로 여러 소셜 계정 연동 지원
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class OAuthService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final UserSocialLinkRepository userSocialLinkRepository;
    private final ReferenceRepository referenceRepository;
    private final Map<SocialProvider, OAuthProvider> providerMap;

    public OAuthService(UserRepository userRepository,
                        UserSocialLinkRepository userSocialLinkRepository,
                        ReferenceRepository referenceRepository,
                        List<OAuthProvider> providers) {
        this.userRepository = userRepository;
        this.userSocialLinkRepository = userSocialLinkRepository;
        this.referenceRepository = referenceRepository;
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

        return new CustomOAuth2User(userInfo.getAttributes(), authorities, userInfo.getUsername(), user.getId(),
                user.getEmail(), user.getRoleType().name());
    }

    /**
     * 사용자 찾기 또는 생성 (계정 연동 지원)
     * <p>
     * 1. UserSocialLink에서 (provider, providerUserId)로 검색 - 있으면: 해당 User 반환 2. 없으면 이메일로 User 검색 - User 있음: 소셜 연동 생성
     * (UserSocialLink) 후 User 반환 - User 없음: 새 User + UserSocialLink 생성
     */
    private User findOrCreateUser(OAuthUserInfo userInfo) {
        SocialProvider provider = userInfo.getProviderType();
        String providerUserId = userInfo.getProviderUserId();

        // 1. 기존 소셜 연동으로 등록된 계정 찾기
        Optional<User> existingUserBySocialLink = userSocialLinkRepository
                .findUserByProviderAndProviderUserId(provider, providerUserId);

        if (existingUserBySocialLink.isPresent()) {
            log.info("기존 소셜 연동 계정으로 로그인 - provider: {}, userId: {}",
                    provider, existingUserBySocialLink.get().getId());
            return existingUserBySocialLink.get();
        }

        // 2. 이메일로 기존 User 검색 (연동 시도)
        if (userInfo.getEmail() != null && !userInfo.getEmail().isBlank()) {
            Optional<User> existingUserByEmail = userRepository.findByEmail(userInfo.getEmail());

            if (existingUserByEmail.isPresent()) {
                User user = existingUserByEmail.get();

                // 소셜 연동 생성
                createSocialLink(user, userInfo);

                log.info("기존 계정에 소셜 연동 추가 - userId: {}, provider: {}", user.getId(), provider);
                return user;
            }
        }

        // 3. 기존 username으로 등록된 소셜 계정 찾기 (하위 호환성)
        Optional<User> existingUserByUsername = userRepository
                .findByUsernameAndIsSocial(userInfo.getUsername(), true);

        if (existingUserByUsername.isPresent()) {
            User user = existingUserByUsername.get();

            // UserSocialLink가 없으면 생성 (마이그레이션)
            if (!userSocialLinkRepository.existsByUserIdAndProvider(user.getId(), provider)) {
                createSocialLink(user, userInfo);
                log.info("기존 소셜 계정에 UserSocialLink 마이그레이션 - userId: {}, provider: {}",
                        user.getId(), provider);
            }

            return user;
        }

        // 4. 새 User 생성
        return createNewUser(userInfo);
    }

    /**
     * 새 사용자 생성 + 소셜 연동 생성 + 기본 미지정 폴더 생성
     */
    private User createNewUser(OAuthUserInfo userInfo) {
        User newUser = User.builder()
                .username(userInfo.getUsername())
                .password("")
                .isLock(false)
                .isSocial(true)
                .socialProvider(userInfo.getProviderType())
                .roleType(UserRole.USER)
                .nickname(null)  // 프로필 완성 시 설정
                .email(userInfo.getEmail())
                .status(UserStatus.PENDING)  // 프로필 미완성 상태
                .build();

        User savedUser = userRepository.save(newUser);
        createSocialLink(savedUser, userInfo); // 소셜 연동 생성
        createDefaultReference(savedUser); // 기본 미지정 폴더 생성

        log.info("새 사용자 생성 - userId: {}, provider: {}, email: {}", savedUser.getId(), userInfo.getProviderType(),
                userInfo.getEmail());

        return savedUser;
    }

    /**
     * 소셜 연동 생성
     */
    private void createSocialLink(User user, OAuthUserInfo userInfo) {
        UserSocialLink socialLink = UserSocialLink.builder().user(user).provider(userInfo.getProviderType())
                .providerUserId(userInfo.getProviderUserId()).build();

        userSocialLinkRepository.save(socialLink);
    }

    /**
     * 기본 미지정 폴더 생성
     */
    private void createDefaultReference(User user) {
        Reference defaultReference = Reference.createDefault(user);
        referenceRepository.save(defaultReference);

        log.info("기본 미지정 폴더 생성 완료 - userId: {}", user.getId());
    }
}