package swyp12.team9.server.api.userlink.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "사용자 링크 수정 요청 객체")
public record UserLinkUpdateRequest(

        @Schema(
                description = "링크를 저장하는 이유",
                example = "좋은 아티클이라서 나중에 다시 읽으려고",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @Size(max = 500, message = "이유는 500자를 초과할 수 없습니다.")
        String why,

        @Schema(
                description = "메모 (생략 시 기존값 유지)",
                example = "핵심 내용: 디자인 패턴에 대한 설명",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String memo,

        @Schema(
                description = "레퍼런스 폴더 ID (생략 시 기존값 유지, moveToDefault와 상호 배타적)",
                example = "1",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        Long referenceId,

        @Schema(
                description = "미지정 폴더로 이동 여부 (true면 미지정 폴더로 이동, referenceId와 상호 배타적)",
                example = "false",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        Boolean moveToDefault
) {
}