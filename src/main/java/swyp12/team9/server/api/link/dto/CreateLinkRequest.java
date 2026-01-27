package swyp12.team9.server.api.link.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.URL;

import java.util.List;

@Schema(description = "링크 생성 요청")
public record CreateLinkRequest(

        @Schema(description = "레퍼런스(폴더) ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "레퍼런스 ID는 필수입니다.")
        Long referenceId,

        @Schema(description = "URL 주소", example = "https://example.com/article", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "URL은 필수입니다.")
        @URL(message = "유효한 URL 형식이어야 합니다.")
        String url,

        @Schema(description = "제목", example = "좋은 아티클 제목", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String title,

        @Schema(description = "썸네일 이미지 URL", example = "https://example.com/thumb.jpg", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String thumbnailUrl,

        @Schema(description = "핵심요약", example = "이 글의 핵심 내용 요약", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String summary,

        @Schema(description = "WHY (저장 이유)", example = "좋은 아티클이라서 나중에 다시 읽으려고", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "WHY는 필수입니다.")
        String why,

        @Schema(description = "메모", example = "핵심 내용 정리", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String memo,

        @Schema(description = "공개 여부", example = "false", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Boolean isPublic,

        @Schema(description = "카테고리 ID 목록 (탐색 탭용)", example = "[1, 2]", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        List<Long> categoryIds
) {
}
