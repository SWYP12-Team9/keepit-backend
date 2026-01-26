package swyp12.team9.server.api.userlink.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import swyp12.team9.server.domain.userlink.model.LinkStatus;
import swyp12.team9.server.domain.userlink.model.UserLink;

import java.time.LocalDateTime;

@Builder
@Schema(description = "사용자 링크 응답 객체")
public record UserLinkResponse(

        @Schema(description = "사용자 링크 ID", example = "1")
        Long id,

        @Schema(description = "링크 ID", example = "10")
        Long linkId,

        @Schema(description = "링크 URL", example = "https://example.com/article")
        String url,

        @Schema(description = "링크 제목", example = "디자인 패턴 가이드")
        String title,

        @Schema(description = "링크 설명", example = "디자인 패턴에 대한 종합 가이드")
        String description,

        @Schema(description = "미리보기 이미지 URL", example = "https://example.com/preview.jpg")
        String previewImageUrl,

        @Schema(description = "읽음 상태", example = "UNREAD")
        LinkStatus status,

        @Schema(description = "저장 이유", example = "좋은 아티클이라서")
        String why,

        @Schema(description = "공개 여부", example = "true")
        Boolean isPublic,

        @Schema(description = "메모", example = "핵심 내용 정리")
        String memo,

        @Schema(description = "조회수", example = "5")
        Long viewCount,

        @Schema(description = "첫 열람 시간", example = "2024-01-15T10:30:00")
        LocalDateTime firstOpenedAt,

        @Schema(description = "마지막 열람 시간", example = "2024-01-20T14:20:00")
        LocalDateTime lastOpenedAt,

        @Schema(description = "생성일시", example = "2024-01-10T09:00:00")
        LocalDateTime createdAt,

        @Schema(description = "수정일시", example = "2024-01-15T11:00:00")
        LocalDateTime updatedAt
) {

    public static UserLinkResponse from(UserLink userLink) {
        return UserLinkResponse.builder()
                .id(userLink.getId())
                .linkId(userLink.getLink().getId())
                .url(userLink.getLink().getUrl())
                .title(userLink.getLink().getTitle())
                .description(userLink.getLink().getDescription())
                .previewImageUrl(userLink.getLink().getPreviewImageUrl())
                .status(userLink.getStatus())
                .why(userLink.getWhy())
                .isPublic(userLink.getIsPublic())
                .memo(userLink.getMemo())
                .viewCount(userLink.getViewCount())
                .firstOpenedAt(userLink.getFirstOpenedAt())
                .lastOpenedAt(userLink.getLastOpenedAt())
                .createdAt(userLink.getCreatedAt())
                .updatedAt(userLink.getUpdatedAt())
                .build();
    }
}