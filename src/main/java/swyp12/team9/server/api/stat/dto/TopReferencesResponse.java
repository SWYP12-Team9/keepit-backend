package swyp12.team9.server.api.stat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
@Schema(description = "상위 레퍼런스 통계 응답")
public record TopReferencesResponse(
        @Schema(description = "기준 날짜 (YYYY.MM.DD)", example = "2026.02.06")
        String date,

        @Schema(description = "상위 레퍼런스 목록")
        List<TopReferenceInfo> references,

        @Schema(description = "인사이트 텍스트", example = "요즘 가장 많이 모아둔 폴더는 '개발'이에요. 링크가 총 15개예요.")
        String text
) {
    /**
     * TopReferencesResponse 생성
     *
     * @param date       기준 날짜
     * @param references 상위 레퍼런스 목록
     * @param text       인사이트 텍스트
     * @return TopReferencesResponse
     */
    public static TopReferencesResponse of(String date, List<TopReferenceInfo> references, String text) {
        return TopReferencesResponse.builder()
                .date(date)
                .references(references)
                .text(text)
                .build();
    }
}
