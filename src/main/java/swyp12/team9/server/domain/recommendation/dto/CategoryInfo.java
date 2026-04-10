package swyp12.team9.server.domain.recommendation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 카테고리 정보
 */
@Schema(description = "카테고리 정보")
public record CategoryInfo(
        @Schema(description = "카테고리 이름", example = "개발")
        String name
) {
    public static CategoryInfo from(String name) {
        return new CategoryInfo(name);
    }
}
