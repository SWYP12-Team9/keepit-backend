package swyp12.team9.server.api.terms.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import swyp12.team9.server.api.terms.dto.TermsType;

@Getter
@Builder
@Schema(description = "약관 응답")
public class TermsResponse {

    @Schema(description = "약관 타입", example = "SERVICE")
    private final TermsType type;

    @Schema(description = "약관 제목", example = "Keepit 서비스 이용약관")
    private final String title;

    @Schema(description = "시행일자", example = "2026년 2월 8일")
    private final String effectiveDate;

    @Schema(description = "약관 내용")
    private final String content;

    public static TermsResponse of(TermsType type, String effectiveDate, String content) {
        return TermsResponse.builder()
                .type(type)
                .title(type.getTitle())
                .effectiveDate(effectiveDate)
                .content(content)
                .build();
    }
}
