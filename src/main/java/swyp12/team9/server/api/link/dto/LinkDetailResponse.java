package swyp12.team9.server.api.link.dto;

import swyp12.team9.server.domain.userlink.model.UserLink;
import swyp12.team9.server.domain.link.model.Link;
import java.time.LocalDateTime;

public record LinkDetailResponse(
    Long userLinkId,
    Long linkId,
    String url,
    String title,
    String description,
    String imageUrl,
    String aiSummary,
    String purpose,
    Boolean isPublic,
    String memo,
    String status,
    Long viewCount,
    LocalDateTime createdAt) {
  public static LinkDetailResponse from(UserLink userLink) {
    Link link = userLink.getLink();
    return new LinkDetailResponse(
        userLink.getId(),
        link.getId(),
        link.getUrl(),
        link.getTitle(),
        link.getDescription(),
        link.getPreviewImageUrl(),
        link.getAiSummary(),
        userLink.getPurpose(),
        userLink.getIsPublic(),
        userLink.getMemo(),
        userLink.getStatus().name(),
        userLink.getViewCount(),
        userLink.getCreatedAt());
  }
}
