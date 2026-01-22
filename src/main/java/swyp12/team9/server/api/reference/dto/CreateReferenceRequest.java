package swyp12.team9.server.api.reference.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "레퍼런스 생성 요청 객체")
public record CreateReferenceRequest(

        @Schema(description = "레퍼런스 제목", example = "경제", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 200, message = "제목은 200자를 초과할 수 없습니다.")
        String title,

        @Schema(description = "레퍼런스 설명", example = "경제 관련 레퍼런스 뷰 폴더입니다.")
        @Size(max = 500, message = "설명은 500자를 초과할 수 없습니다.")
        String description,

        @Schema(description = "레퍼런스 공개여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean isPublic
) {

}
