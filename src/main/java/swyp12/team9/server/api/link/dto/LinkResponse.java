package swyp12.team9.server.api.link.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.link.model.ViewStatus;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Schema(description = "링크 응답")
public record LinkResponse(

        @Schema(description = "링크 ID", example = "1")
        Long id,

        @Schema(description = "레퍼런스(폴더) ID", example = "1")
        Long referenceId,

        @Schema(description = "레퍼런스(폴더) 제목", example = "개발 자료")
        String referenceTitle,

        @Schema(description = "URL 주소", example = "https://example.com/article")
        String url,

        @Schema(description = "제목", example = "좋은 아티클 제목")
        String title,

        @Schema(description = "썸네일 이미지 URL", example = "https://example.com/thumb.jpg")
        String thumbnailUrl,

        @Schema(description = "핵심요약", example = "이 글의 핵심 내용 요약")
        String summary,

        @Schema(description = "WHY (저장 이유)", example = "좋은 아티클이라서")
        String why,

        @Schema(description = "메모", example = "핵심 내용 정리")
        String memo,

        @Schema(description = "열람 상태", example = "NOT_VIEWED")
        ViewStatus viewStatus,

        @Schema(description = "즐겨찾기 여부", example = "false")
        Boolean isBookmarked,

        @Schema(description = "공개 여부", example = "false")
        Boolean isPublic,

        @Schema(description = "카테고리 목록", example = "[\"경제/시사\", \"직장/자기개발\"]")
        List<String> categories,

        @Schema(description = "생성일시", example = "2024-01-10T09:00:00")
        LocalDateTime createdAt,

        @Schema(description = "수정일시", example = "2024-01-15T11:00:00")
        LocalDateTime updatedAt
) {

    public static LinkResponse from(Link link, List<String> categoryNames) {
        return LinkResponse.builder()
                .id(link.getId())
                .referenceId(link.getReference().getId())
                .referenceTitle(link.getReference().getTitle())
                .url(link.getUrl())
                .title(link.getTitle())
                .thumbnailUrl(link.getThumbnailUrl())
                .summary(link.getSummary())
                .why(link.getWhy())
                .memo(link.getMemo())
                .viewStatus(link.getViewStatus())
                .isBookmarked(link.getIsBookmarked())
                .isPublic(link.getIsPublic())
                .categories(categoryNames)
                .createdAt(link.getCreatedAt())
                .updatedAt(link.getUpdatedAt())
                .build();
    }
}
