package swyp12.team9.server.domain.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import swyp12.team9.server.domain.user.model.SocialProvider;
import swyp12.team9.server.domain.user.model.User;
import swyp12.team9.server.domain.user.model.UserSocialLink;

import java.util.List;
import java.util.Optional;

public interface UserSocialLinkRepository extends JpaRepository<UserSocialLink, Long> {

    /**
     * 소셜 제공자와 제공자 사용자 ID로 소셜 링크 조회
     */
    Optional<UserSocialLink> findByProviderAndProviderUserId(SocialProvider provider, String providerUserId);

    /**
     * 소셜 제공자와 제공자 사용자 ID로 User를 직접 조회 (fetch join)
     */
    @Query("SELECT sl.user FROM UserSocialLink sl WHERE sl.provider = :provider AND sl.providerUserId = :providerUserId")
    Optional<User> findUserByProviderAndProviderUserId(
            @Param("provider") SocialProvider provider,
            @Param("providerUserId") String providerUserId);

    /**
     * 특정 User의 모든 소셜 연동 조회
     */
    List<UserSocialLink> findByUserId(Long userId);

    /**
     * 특정 User가 특정 소셜 제공자와 연동되어 있는지 확인
     */
    boolean existsByUserIdAndProvider(Long userId, SocialProvider provider);

    /**
     * 특정 User의 특정 소셜 연동 삭제
     */
    void deleteByUserIdAndProvider(Long userId, SocialProvider provider);
}