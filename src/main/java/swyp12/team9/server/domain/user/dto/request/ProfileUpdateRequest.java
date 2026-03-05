package swyp12.team9.server.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Schema(description = "프로필 수정 요청 - 변경할 필드만 전송")
@Builder
public record ProfileUpdateRequest(

        @Size(min = 2, max = 10, message = "닉네임은 2~10자여야 합니다")
        @Schema(description = "닉네임 (선택) - null이면 기존 값 유지", example = "개발자준현")
        String nickname,

        @Size(max = 40, message = "한 줄 소개는 40자 이하여야 합니다")
        @Schema(description = "한 줄 소개 (선택) - null이면 기존 값 유지", example = "백엔드 개발자입니다")
        String introduction
) {

}