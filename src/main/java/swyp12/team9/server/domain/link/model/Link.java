package swyp12.team9.server.domain.link.model;

import jakarta.persistence.*;
import lombok.*;
import swyp12.team9.server.global.common.entity.BaseEntity;

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

    @Column(name = "url", nullable = false, length = 2048)
    private String url;

    @Column(name = "title", length = 300)
    private String title;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "preview_image_url", length = 1024)
    private String previewImageUrl;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 50)
    private LinkCategory category;

    @Builder
    public Link(String url, String title, String description, String previewImageUrl, String aiSummary, LinkCategory category) {
        this.url = url;
        this.title = title;
        this.description = description;
        this.previewImageUrl = previewImageUrl;
        this.aiSummary = aiSummary;
        this.category = category;
    }
}