package swyp12.team9.server.domain.link.model;

import jakarta.persistence.*;
import lombok.*;
import swyp12.team9.server.domain.link.exception.LinkAccessDeniedException;
import swyp12.team9.server.domain.reference.model.Reference;
import swyp12.team9.server.global.common.entity.BaseEntity;

/**
 * 링크 엔티티 (LINK 테이블)
 * - 사용자가 저장한 링크 정보
 * - Reference(레퍼런스 폴더)에 속함
 */
@Entity
@Table(name = "links")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Link extends BaseEntity {

    @Id
    @Column(name = "link_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reference_id", nullable = false)
    private Reference reference;

    @Column(name = "url", nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "why", nullable = false, columnDefinition = "TEXT")
    private String why;

    @Column(name = "memo", columnDefinition = "TEXT")
    private String memo;

    @Enumerated(EnumType.STRING)
    @Column(name = "view_status", nullable = false, length = 20)
    private ViewStatus viewStatus;

    @Column(name = "is_bookmarked", nullable = false)
    private Boolean isBookmarked;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic;

    @Builder
    public Link(Reference reference, String url, String title, String thumbnailUrl, String summary,
                String why, String memo, Boolean isPublic) {
        this.reference = reference;
        this.url = url;
        this.title = title != null ? title : url;  // 기본값은 URL
        this.thumbnailUrl = thumbnailUrl;
        this.summary = summary;
        this.why = why;
        this.memo = memo;
        this.viewStatus = ViewStatus.NOT_VIEWED;
        this.isBookmarked = false;
        this.isPublic = isPublic != null ? isPublic : false;
    }

    /**
     * 링크 정보 수정
     */
    public void update(String title, String thumbnailUrl, String summary, String why, String memo, Boolean isPublic) {
        if (title != null) this.title = title;
        if (thumbnailUrl != null) this.thumbnailUrl = thumbnailUrl;
        if (summary != null) this.summary = summary;
        if (why != null) this.why = why;
        if (memo != null) this.memo = memo;
        if (isPublic != null) this.isPublic = isPublic;
    }

    /**
     * 열람 처리 (NOT_VIEWED → VIEWED)
     */
    public void markAsViewed() {
        this.viewStatus = ViewStatus.VIEWED;
    }

    /**
     * 즐겨찾기 토글
     */
    public void toggleBookmark() {
        this.isBookmarked = !this.isBookmarked;
    }

    /**
     * 소유자 검증 (Reference의 소유자와 비교)
     */
    public void validateOwner(Long userId) {
        if (!this.reference.getUser().getId().equals(userId)) {
            throw new LinkAccessDeniedException("해당 링크에 대한 권한이 없습니다.");
        }
    }

    /**
     * 소유자 ID 반환
     */
    public Long getOwnerId() {
        return this.reference.getUser().getId();
    }
}