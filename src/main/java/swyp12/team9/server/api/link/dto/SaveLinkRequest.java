package swyp12.team9.server.api.link.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "링크 저장 요청 객체")
public record SaveLinkRequest(
        @Schema(description = "레퍼런스(폴더) 아이디", example = "1", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull(message = "폴더 아이디는 필수입니다") Long referenceId,

        @Schema(description = "링크 URL", example = "https://www.example.com", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank(message = "URL은 필수입니다") String url,
        @Schema(description = "저장 목적", example = "학습", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank(message = "저장 목적은 필수입니다") String purpose,
        @Schema(description = "저장 이유", example = "나중에 읽기 위해") String why,

        @Schema(description = "메모", example = "중요한 내용 포함") String memo) {
}
