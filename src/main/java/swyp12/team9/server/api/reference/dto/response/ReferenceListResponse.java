package swyp12.team9.server.api.reference.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import swyp12.team9.server.domain.reference.model.Reference;

@Builder
@Schema(description = "레퍼런스 목록 조회용 요약 응답 객체")
public record ReferenceListResponse(

        @Schema(description = "레퍼런스 아이디", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Long id,

        @Schema(description = "레퍼런스 제목", example = "경제", requiredMode = Schema.RequiredMode.REQUIRED)
        String title,

        @Schema(description = "레퍼런스 색상 코드", example = "#FF5733")
        String colorCode,

        @Schema(description = "연결된 링크 개수", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
        Long linkCount,

        @Schema(description = "기본 미지정 폴더 여부", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean isDefault

) {

    /**
     * Entity를 ReferenceListResponse로 변환
     * @param reference 레퍼런스 엔티티
     * @param linkCount 해당 레퍼런스에 포함된 링크 총 개수
     */
    public static ReferenceListResponse of(Reference reference, Long linkCount) {
        return ReferenceListResponse.builder()
                .id(reference.getId())
                .title(reference.getTitle())
                .colorCode(reference.getColorCode())
                .linkCount(linkCount)
                .isDefault(reference.getIsDefault())
                .build();
    }
}