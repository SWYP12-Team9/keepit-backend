package swyp12.team9.server.api.reference.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "레퍼런스 생성 요청 객체")
public record ReferenceCreateRequest(

        @Schema(
                description = "레퍼런스 제목",
                example = "경제",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 200, message = "제목은 200자를 초과할 수 없습니다.")
        String title,

        @Schema(description = "레퍼런스 설명", example = "경제 관련 레퍼런스 뷰 폴더입니다.")
        @Size(max = 500, message = "설명은 500자를 초과할 수 없습니다.")
        String description,

        @Schema(description = "레퍼런스 공개여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "공개여부는 필수입니다.")
        Boolean isPublic,

        @Schema(description = "레퍼런스 색상 코드", example = "#FF5733")
        @Size(max = 7, message = "색상 코드는 7자를 초과할 수 없습니다.")
        String colorCode
) {

}
