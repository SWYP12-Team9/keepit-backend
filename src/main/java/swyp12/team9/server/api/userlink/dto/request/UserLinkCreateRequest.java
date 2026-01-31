package swyp12.team9.server.api.userlink.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.util.List;

@Schema(description = "사용자 링크 생성 요청 객체")
public record UserLinkCreateRequest(

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
                description = "레퍼런스 폴더 ID 목록 (null 또는 빈 배열이면 미지정 폴더로 자동 분류)",
                example = "[1, 2, 3]",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        List<Long> referenceIds,

        @Schema(
                description = "메모",
                example = "핵심 내용: 경제 패턴에 대한 설명",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        String memo
) {
}