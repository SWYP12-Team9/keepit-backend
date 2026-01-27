package swyp12.team9.server.domain.category.model;

import jakarta.persistence.*;
import lombok.*;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.global.common.entity.BaseEntity;

/**
 * 카테고리-링크 연관 테이블 (N:M 관계)
 */
@Entity
@Table(name = "category_links")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CategoryLink extends BaseEntity {

    @Id
    @Column(name = "category_link_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "link_id", nullable = false)
    private Link link;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Builder
    public CategoryLink(Link link, Category category) {
        this.link = link;
        this.category = category;
    }
}
