package swyp12.team9.server.domain.reference.model;

import jakarta.persistence.*;
import lombok.*;
import swyp12.team9.server.domain.user.model.User;
import swyp12.team9.server.global.common.entity.BaseEntity;

@Entity
@Table(name = "references")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reference extends BaseEntity {

    @Id
    @Column(name = "reference_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic;

    @Builder
    public Reference(User user, String title, String description, Boolean isPublic) {
        this.user = user;
        this.title = title;
        this.description = description;
        this.isPublic = isPublic;
    }
}