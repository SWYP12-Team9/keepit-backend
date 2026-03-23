package swyp12.team9.server.domain.link.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import swyp12.team9.server.domain.link.exception.LinkHashGenerationException;
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

    @Column(name = "url", nullable = false, columnDefinition = "TEXT")
    private String url;

    // url_hash(SHA-256)로 전체 URL에 대한 UNIQUE 보장
    @Column(name = "url_hash", nullable = false, length = 64, unique = true)
    private String urlHash;

    @Column(name = "title", length = 300)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "favicon_url", length = 1024)
    private String faviconUrl;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Column(name = "public_view_count", nullable = false)
    private Long publicViewCount = 0L;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 20)
    private LinkProcessingStatus processingStatus;

    @Builder
    public Link(
            String url,
            String title,
            String description,
            String faviconUrl,
            String content,
            String aiSummary,
            LinkProcessingStatus processingStatus
    ) {
        this.url = url;
        this.urlHash = generateUrlHash(url);
        this.title = title;
        this.description = description;
        this.faviconUrl = faviconUrl;
        this.content = content;
        this.aiSummary = aiSummary;
        this.processingStatus = processingStatus != null ? processingStatus : LinkProcessingStatus.PENDING;
        this.publicViewCount = 0L;
    }

    public static Link create(String url, String title, String description, String faviconUrl, String content) {
        return Link.builder()
                .url(url)
                .title(title)
                .description(description)
                .faviconUrl(faviconUrl)
                .content(content)
                .processingStatus(LinkProcessingStatus.READY)
                .build();
    }

    public static Link createPlaceholder(String url) {
        return Link.builder()
                .url(url)
                .processingStatus(LinkProcessingStatus.PENDING)
                .build();
    }

    public void updateAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }

    public void incrementPublicViewCount() {
        this.publicViewCount++;
    }

    public void updateFromScraping(String title, String description, String faviconUrl, String content) {
        this.title = title;
        this.description = description;
        this.faviconUrl = faviconUrl;
        this.content = content;
    }

    public void complete(String title, String description, String faviconUrl, String content, String aiSummary) {
        this.title = title;
        this.description = description;
        this.faviconUrl = faviconUrl;
        this.content = content;
        this.aiSummary = aiSummary;
        this.processingStatus = LinkProcessingStatus.READY;
    }

    public void markPending() {
        this.processingStatus = LinkProcessingStatus.PENDING;
    }

    public void markFailed() {
        this.processingStatus = LinkProcessingStatus.FAILED;
    }

    public boolean isPending() {
        return this.processingStatus == LinkProcessingStatus.PENDING;
    }

    public boolean isReady() {
        return this.processingStatus == LinkProcessingStatus.READY;
    }

    public boolean isFailed() {
        return this.processingStatus == LinkProcessingStatus.FAILED;
    }

    public boolean hasAiSummary() {
        return this.aiSummary != null && !this.aiSummary.isBlank();
    }

    public boolean isContentChanged(String newTitle, String newDescription, String newContent) {
        return !Objects.equals(this.title, newTitle)
                || !Objects.equals(this.description, newDescription)
                || !Objects.equals(this.content, newContent);
    }

    public boolean isScrapedDataChanged(String newTitle, String newDescription, String newFaviconUrl, String newContent) {
        return !Objects.equals(this.title, newTitle)
                || !Objects.equals(this.description, newDescription)
                || !Objects.equals(this.faviconUrl, newFaviconUrl)
                || !Objects.equals(this.content, newContent);
    }

    public static String generateUrlHash(String url) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(url.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new LinkHashGenerationException(e);
        }
    }
}