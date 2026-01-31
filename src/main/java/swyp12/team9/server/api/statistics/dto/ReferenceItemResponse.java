package swyp12.team9.server.api.statistics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * 레퍼런스 항목 응답 DTO
 */
@Builder
@Schema(description = "레퍼런스 항목")
public record ReferenceItemResponse(
        @Schema(description = "레퍼런스 ID", example = "1")
        Long referenceId,

        @Schema(description = "레퍼런스 이름", example = "개발 자료")
        String referenceName,

        @Schema(description = "레퍼런스에 담긴 링크 개수", example = "25")
        Long linkCount,

        @Schema(description = "레퍼런스 색상 코드", example = "#FF5733")
        String colorCode
) {
}
