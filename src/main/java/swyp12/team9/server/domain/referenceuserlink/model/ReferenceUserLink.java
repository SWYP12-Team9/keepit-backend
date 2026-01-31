package swyp12.team9.server.domain.referenceuserlink.model;

import jakarta.persistence.*;
import lombok.*;
import swyp12.team9.server.domain.reference.model.Reference;
import swyp12.team9.server.domain.userlink.model.UserLink;
import swyp12.team9.server.global.common.entity.BaseEntity;

@Entity
@Table(name = "reference_user_links")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReferenceUserLink extends BaseEntity {

    @Id
    @Column(name = "reference_user_link_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reference_id", nullable = false)
    private Reference reference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_link_id", nullable = false)
    private UserLink userLink;

    @Builder
    public ReferenceUserLink(Reference reference, UserLink userLink) {
        this.reference = reference;
        this.userLink = userLink;
    }

    /**
     * ReferenceUserLink 생성
     *
     * @param reference Reference 엔티티
     * @param userLink  UserLink 엔티티
     * @return 생성된 ReferenceUserLink
     */
    public static ReferenceUserLink create(Reference reference, UserLink userLink) {
        return ReferenceUserLink.builder()
                .reference(reference)
                .userLink(userLink)
                .build();
    }
}