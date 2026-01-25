package swyp12.team9.server.domain.bookmark.model;

import jakarta.persistence.*;
import lombok.*;
import swyp12.team9.server.domain.user.model.User;
import swyp12.team9.server.domain.userlink.model.UserLink;
import swyp12.team9.server.global.common.entity.BaseEntity;

@Entity
@Table(name = "bookmarks")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bookmark extends BaseEntity {

    @Id
    @Column(name = "bookmark_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_link_id", nullable = false)
    private UserLink userLink;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder
    public Bookmark(UserLink userLink, User user) {
        this.userLink = userLink;
        this.user = user;
    }
}