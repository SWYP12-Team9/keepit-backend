package swyp12.team9.server.domain.stat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
@Schema(description = "저장 패턴 통계 응답")
public record SavePatternResponse(
        @Schema(description = "기간 (YYYY.MM.DD ~ YYYY.MM.DD)", example = "2026.01.09 ~ 2026.02.06")
        String period,

        @Schema(description = "가장 많이 저장한 요일 (단독 1위인 경우만 값 존재, 동률이거나 데이터 없으면 null)", example = "월", nullable = true)
        String peakDay,

        @Schema(description = "요일별 저장 개수", example = """
                [
                  {"day": "월", "linkCount": 10},
                  {"day": "화", "linkCount": 5},
                  {"day": "수", "linkCount": 8},
                  {"day": "목", "linkCount": 12},
                  {"day": "금", "linkCount": 7},
                  {"day": "토", "linkCount": 3},
                  {"day": "일", "linkCount": 2}
                ]
                """)
        List<DayLinkCountResponse> counts,

        @Schema(description = "인사이트 텍스트", example = "'월요일'에 링크를 가장 많이 저장했어요. 총 10개예요.")
        String text
) {
    /**
     * SavePatternResponse 생성
     *
     * @param period  기간
     * @param peakDay 가장 많이 저장한 요일 (단독 1위만 값 존재, 동률이거나 데이터 없으면 null)
     * @param counts  요일별 저장 개수
     * @param text    인사이트 텍스트
     * @return SavePatternResponse
     */
    public static SavePatternResponse of(String period, String peakDay, List<DayLinkCountResponse> counts, String text) {
        return SavePatternResponse.builder()
                .period(period)
                .peakDay(peakDay)
                .counts(counts)
                .text(text)
                .build();
    }
}
