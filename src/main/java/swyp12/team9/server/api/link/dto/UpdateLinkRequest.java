package swyp12.team9.server.api.link.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "링크 수정 요청")
public record UpdateLinkRequest(

        @Schema(description = "제목", example = "수정된 제목", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String title,

        @Schema(description = "썸네일 이미지 URL", example = "https://example.com/new-thumb.jpg", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String thumbnailUrl,

        @Schema(description = "핵심요약", example = "수정된 요약", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String summary,

        @Schema(description = "WHY (저장 이유)", example = "수정된 이유", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String why,

        @Schema(description = "메모", example = "수정된 메모", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String memo,

        @Schema(description = "공개 여부", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Boolean isPublic,

        @Schema(description = "카테고리 ID 목록 (탐색 탭용)", example = "[1, 3]", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        List<Long> categoryIds
) {
}
