package swyp12.team9.server.domain.popular.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "인기 콘텐츠 카테고리 정보")
public record PopularCategoryInfo(
        @Schema(description = "카테고리 표시 이름", example = "경제/시사")
        String name
) {
    public static PopularCategoryInfo from(String name) {
        return new PopularCategoryInfo(name);
    }
}
