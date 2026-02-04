package swyp12.team9.server.domain.user.fixture;

import swyp12.team9.server.domain.user.model.SocialProvider;
import swyp12.team9.server.domain.user.model.User;
import swyp12.team9.server.domain.user.model.UserRole;
import swyp12.team9.server.domain.user.model.UserStatus;

public class UserFixture {

    public static final Long USER_ID = 1L;
    public static final Long OTHER_USER_ID = 2L;

    public static final String USERNAME = "testuser";
    public static final String PASSWORD = "password123";
    public static final String NICKNAME = "테스트유저";
    public static final String EMAIL = "test@example.com";
    public static final String INTRODUCTION = "안녕하세요";

    /**
     * 기본 User 엔티티 생성
     */
    public static User createUser() {
        return User.builder()
                .username(USERNAME)
                .password(PASSWORD)
                .nickname(NICKNAME)
                .email(EMAIL)
                .introduction(INTRODUCTION)
                .isLock(false)
                .isSocial(false)
                .socialProvider(null)
                .roleType(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
    }

    /**
     * ID가 설정된 User 생성 (테스트용)
     */
    public static User createUserWithId(Long id) {
        return new User(
                id,
                USERNAME,
                PASSWORD,
                false,
                false,
                null,
                UserRole.USER,
                UserStatus.ACTIVE,
                NICKNAME,
                INTRODUCTION,
                null,
                null,
                EMAIL
        );
    }

    /**
     * 다른 사용자 생성 (권한 테스트용)
     */
    public static User createOtherUser() {
        return User.builder()
                .username("otheruser")
                .password(PASSWORD)
                .nickname("다른유저")
                .email("other@example.com")
                .introduction(INTRODUCTION)
                .isLock(false)
                .isSocial(false)
                .socialProvider(null)
                .roleType(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
    }

    /**
     * ID가 설정된 다른 사용자 생성
     */
    public static User createOtherUserWithId(Long id) {
        return new User(
                id,
                "otheruser",
                PASSWORD,
                false,
                false,
                null,
                UserRole.USER,
                UserStatus.ACTIVE,
                "다른유저",
                INTRODUCTION,
                null,
                null,
                "other@example.com"
        );
    }

    /**
     * 소셜 로그인 사용자 생성
     */
    public static User createSocialUser() {
        return User.builder()
                .username("socialuser")
                .password("")
                .nickname("소셜유저")
                .email("social@example.com")
                .introduction(INTRODUCTION)
                .isLock(false)
                .isSocial(true)
                .socialProvider(SocialProvider.KAKAO)
                .roleType(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
    }
}