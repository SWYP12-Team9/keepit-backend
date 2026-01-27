package swyp12.team9.server.domain.referenceuserlink.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.reference.model.Reference;
import swyp12.team9.server.global.common.entity.BaseEntity;

/**
 * 레퍼런스-링크 연관 테이블 - 하나의 링크가 여러 레퍼런스 폴더에 속할 수 있는 경우 사용
 */
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
    @JoinColumn(name = "link_id", nullable = false)
    private Link link;

    @Builder
    public ReferenceUserLink(Reference reference, Link link) {
        this.reference = reference;
        this.link = link;
    }
}