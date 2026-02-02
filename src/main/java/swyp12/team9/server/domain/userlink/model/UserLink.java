package swyp12.team9.server.domain.userlink.model;

import jakarta.persistence.*;
import lombok.*;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.user.model.User;
import swyp12.team9.server.domain.userlink.exception.UserLinkAccessDeniedException;
import swyp12.team9.server.global.common.entity.BaseEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_links")
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

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic;

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
        this.isPublic = false;
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
        this.viewCount++;
    }

    public void incrementViewCount() {
        this.viewCount++;
        this.lastOpenedAt = LocalDateTime.now();
    }

    public void validateOwner(Long userId) {
        if (!this.user.getId().equals(userId)) {
            throw new UserLinkAccessDeniedException();
        }
    }
}