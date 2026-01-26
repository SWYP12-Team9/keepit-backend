package swyp12.team9.server.api.userlink.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

@Schema(description = "사용자 링크 생성 요청 객체")
public record CreateUserLinkRequest(

        @Schema(
                description = "링크를 저장하는 이유",
                example = "좋은 아티클이라서 나중에 다시 읽으려고",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @Size(max = 500, message = "이유는 500자를 초과할 수 없습니다.")
        String why,

        @Schema(
                description = "저장할 링크 URL",
                example = "https://example.com/article",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "URL은 필수입니다.")
        @URL(message = "유효한 URL 형식이어야 합니다.")
        String url,

        @Schema(
                description = "공개 여부",
                example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "공개여부는 필수입니다.")
        Boolean isPublic,

        @Schema(
                description = "레퍼런스 폴더 선택",
                example = "경제",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String referenceType,

        @Schema(
                description = "메모",
                example = "핵심 내용: 경제 패턴에 대한 설명",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String memo
) {
}