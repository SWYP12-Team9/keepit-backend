package swyp12.team9.server.domain.userlink.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.user.model.User;
import swyp12.team9.server.domain.userlink.exception.UserLinkAccessDeniedException;
import swyp12.team9.server.global.common.entity.BaseEntity;

@Entity
@Table(name = "user_links", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_link", columnNames = {"user_id", "link_id"})
})
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserLink extends BaseEntity {

    @Id
    @Column(name = "user_link_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "link_id", nullable = false)
    private Link link;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LinkStatus status;

    @Column(name = "why", length = 500)
    private String why;

    @Column(name = "memo", columnDefinition = "TEXT")
    private String memo;

    @Column(name = "view_count", nullable = false)
    private Long viewCount;

    @Column(name = "first_opened_at")
    private LocalDateTime firstOpenedAt;

    @Column(name = "last_opened_at")
    private LocalDateTime lastOpenedAt;

    @Builder
    public UserLink(User user, Link link, String why, String memo) {
        this.user = user;
        this.link = link;
        this.status = LinkStatus.UNREAD;
        this.why = why;
        this.memo = memo;
        this.viewCount = 0L;
    }

    public static UserLink create(User user, Link link, String why, String memo) {
        return UserLink.builder()
                .user(user)
                .link(link)
                .why(why)
                .memo(memo)
                .build();
    }

    public void updateUserLink(String why, String memo) {
        this.why = why;
        this.memo = memo;
    }

    public void markAsRead() {
        if (this.status == LinkStatus.UNREAD) {
            this.status = LinkStatus.READ;
            this.firstOpenedAt = LocalDateTime.now();
        }
        this.lastOpenedAt = LocalDateTime.now();
    }

    public void validateOwner(Long userId) {
        if (!this.user.getId().equals(userId)) {
            throw new UserLinkAccessDeniedException();
        }
    }
}