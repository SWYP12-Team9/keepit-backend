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

    @Column(name = "favicon_url", length = 1024)
    private String faviconUrl;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Builder
    public Link(String url, String title, String description, String faviconUrl, String content, String aiSummary) {
        this.url = url;
        this.title = title;
        this.description = description;
        this.faviconUrl = faviconUrl;
        this.content = content;
        this.aiSummary = aiSummary;
    }

    public static Link create(String url, String title, String description, String faviconUrl, String content) {
        return Link.builder()
                .url(url)
                .title(title)
                .description(description)
                .faviconUrl(faviconUrl)
                .content(content)
                .build();
    }

    public void updateAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }
}