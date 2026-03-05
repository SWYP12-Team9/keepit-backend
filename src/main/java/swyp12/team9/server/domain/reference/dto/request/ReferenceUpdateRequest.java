package swyp12.team9.server.domain.reference.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "레퍼런스 수정 요청 객체")
public record ReferenceUpdateRequest(

        @Schema(
                description = "레퍼런스 제목",
                example = "경제학 공부",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @Size(max = 200, message = "제목은 200자를 초과할 수 없습니다.")
        String title,

        @Schema(
                description = "레퍼런스 설명",
                example = "경제학 관련 레퍼런스 뷰 폴더입니다.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 500, message = "설명은 500자를 초과할 수 없습니다.")
        String description,

        @Schema(
                description = "레퍼런스 공개여부(생략 시 기존값 유지)",
                example = "true",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        Boolean isPublic,

        @Schema(
                description = "레퍼런스 색상 코드(생략 시 기존값 유지)",
                example = "#FF5733",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @Size(max = 7, message = "색상 코드는 7자를 초과할 수 없습니다.")
        String colorCode

) {
}