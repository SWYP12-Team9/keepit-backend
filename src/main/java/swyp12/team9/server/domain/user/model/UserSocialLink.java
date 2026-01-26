package swyp12.team9.server.domain.user.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import swyp12.team9.server.global.common.entity.BaseEntity;

/**
 * 사용자 소셜 계정 연동 엔티티
 * 한 User가 여러 소셜 계정(카카오, 네이버, 구글)을 연동할 수 있음
 */
@Entity
@Table(name = "user_social_links",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_provider_provider_user_id",
                columnNames = {"provider", "provider_user_id"}
        ))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSocialLink extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "social_link_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private SocialProvider provider;

    @Column(name = "provider_user_id", nullable = false, length = 100)
    private String providerUserId;

    @Builder
    public UserSocialLink(User user, SocialProvider provider, String providerUserId) {
        this.user = user;
        this.provider = provider;
        this.providerUserId = providerUserId;
    }
}