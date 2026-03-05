package swyp12.team9.server.domain.userlink.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import swyp12.team9.server.domain.reference.model.Reference;

@Builder
@Schema(description = "레퍼런스 정보")
public record ReferenceInfo(

        @Schema(description = "레퍼런스 ID", example = "10")
        Long id,

        @Schema(description = "레퍼런스 제목", example = "링크가 포함된 레퍼런스 명")
        String title,

        @Schema(description = "레퍼런스 색상 코드", example = "#FF5733")
        String colorCode,

        @Schema(description = "기본 미지정 폴더 여부", example = "false")
        Boolean isDefault
) {
    public static ReferenceInfo from(Reference reference) {
        if (reference == null) {
            return null;
        }

        return ReferenceInfo.builder()
                .id(reference.getId())
                .title(reference.getTitle())
                .colorCode(reference.getColorCode())
                .isDefault(reference.getIsDefault())
                .build();
    }
}
