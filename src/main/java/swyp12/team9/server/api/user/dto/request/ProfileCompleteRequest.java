package swyp12.team9.server.api.user.dto.request;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Schema(description = "프로필 완성 요청")
@Builder
public record ProfileCompleteRequest(

        @NotBlank(message = "닉네임은 필수입니다")
        @Size(min = 2, max = 50, message = "닉네임은 2~10자여야 합니다")
        @Schema(description = "닉네임 (필수)", example = "개발자준현")
        String nickname,

        @Size(max = 300, message = "한 줄 소개는 40자 이하여야 합니다")
        @Schema(description = "한 줄 소개 (선택)", example = "백엔드 개발자입니다")
        String introduction
) {

}