package swyp12.team9.server.domain.userlink.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 링크 생성 시 새 레퍼런스 폴더를 함께 생성하기 위한 요청 객체
 */
@Schema(description = "새 레퍼런스 폴더 생성 요청 (링크 생성 시 함께 사용)")
public record NewReferenceRequest(

        @Schema(
                description = "레퍼런스 폴더 제목",
                example = "개발 공부",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "레퍼런스 제목은 필수입니다.")
        @Size(max = 50, message = "레퍼런스 제목은 50자를 초과할 수 없습니다.")
        String title,

        @Schema(
                description = "레퍼런스 폴더 색상 코드 (HEX 형식)",
                example = "#FF5733",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "유효한 HEX 색상 코드 형식이어야 합니다.")
        String colorCode
) {
}