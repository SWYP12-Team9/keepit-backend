package swyp12.team9.server.domain.user.model;

import jakarta.persistence.*;
import lombok.*;
import swyp12.team9.server.api.user.dto.request.UserRequest;
import swyp12.team9.server.global.common.entity.BaseEntity;

@Entity
@Table(name = "users")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", unique = true, nullable = false, updatable = false)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(length = 50)
    private String name;

    @Column(name = "is_lock", nullable = false)
    private Boolean isLock; // 계정이 잠겼는지, 안 잠겼는지

    @Column(name = "is_social", nullable = false)
    private Boolean isSocial; // 소셜로그인 및 자체로그인 여부

    @Enumerated(EnumType.STRING)
    @Column(name = "social_provider_type")
    private SocialProvider socialProvider; // 소셜 로그인 : 카카오, 네이버, 구글 , 자체로그인 : null

    @Enumerated(EnumType.STRING)
    @Column(name = "role_type", nullable = false)
    private UserRole roleType; // 스프링 시큐리티에서 사용 (일반 회원 or 관리자)

//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false, length = 20)
//    private UserStatus status;

    @Column(name = "nickname", length = 50)
    private String nickname;

    @Column(name = "introduction", length = 300)
    private String introduction;

    @Column(name = "profile_image_url", length = 1024)
    private String profileImageUrl;

    @Column(name = "email", unique = true, length = 100)
    private String email;

    @Builder
    public User(String username, String password, String name, String nickname,
                String introduction, String profileImageUrl, String email,
                Boolean isLock, Boolean isSocial, SocialProvider socialProvider, UserRole roleType) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.nickname = nickname;
        this.introduction = introduction;
        this.profileImageUrl = profileImageUrl;
        this.email = email;
        this.isLock = isLock;
        this.isSocial = isSocial;
        this.socialProvider = socialProvider;
        this.roleType = roleType;
    }

    // 회원 정보 수정
    public void updateUser(UserRequest userRequest) {
        this.email = userRequest.getEmail();
        this.nickname = userRequest.getNickname();
    }

    public void updatePassword(String password) {
        this.password = password;
    }

    public void updateName(String name) {
        this.name = name;
    }

//    public void deactivate() {
//        this.status = UserStatus.INACTIVE;
//    }
//
//    public void activate() {
//        this.status = UserStatus.ACTIVE;
//    }
}