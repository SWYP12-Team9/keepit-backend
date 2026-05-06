package swyp12.team9.server.domain.recommendation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 카테고리 정보
 */
@Schema(description = "카테고리 정보")
public record CategoryInfo(

        @Schema(description = "카테고리명", example = "경제/시사")
        String name
) {
    public static CategoryInfo from(String categoryName) {
        return new CategoryInfo(categoryName);
    }
}
