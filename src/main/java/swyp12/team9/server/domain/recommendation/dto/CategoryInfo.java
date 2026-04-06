package swyp12.team9.server.domain.recommendation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 카테고리 정보
 */
import lombok.*;

/**
 * 카테고리 정보
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "카테고리 정보")
public class CategoryInfo {

    @Schema(description = "카테고리명", example = "경제/시사")
    private String name;

    public static CategoryInfo from(String categoryName) {
        return CategoryInfo.builder()
                .name(categoryName)
                .build();
    }
}
