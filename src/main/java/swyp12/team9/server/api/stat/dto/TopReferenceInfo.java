package swyp12.team9.server.api.stat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import swyp12.team9.server.domain.referenceuserlink.dto.ReferenceCategoryCountProjection;

@Builder
@Schema(description = "상위 레퍼런스 항목")
public record TopReferenceInfo(
        @Schema(description = "순위", example = "1")
        Integer rank,

        @Schema(description = "레퍼런스 ID", example = "10")
        Long referenceId,

        @Schema(description = "레퍼런스 제목", example = "개발")
        String title,

        @Schema(description = "색상 코드", example = "#FF5733")
        String colorCode,

        @Schema(description = "링크 개수", example = "15")
        Long linkCount
) {
        /**
         * ReferenceCategoryCountProjection으로부터 TopReferenceInfo 생성
         *
         * @param rank       순위
         * @param projection 레퍼런스 카테고리 개수 Projection
         * @return TopReferenceInfo
         */
        public static TopReferenceInfo of(Integer rank, ReferenceCategoryCountProjection projection) {
                return TopReferenceInfo.builder()
                        .rank(rank)
                        .referenceId(projection.getReferenceId())
                        .title(projection.getReferenceTitle())
                        .colorCode(projection.getReferenceColorCode() != null ? projection.getReferenceColorCode() : "#CCCCCC")
                        .linkCount(projection.getCount())
                        .build();
        }
}
