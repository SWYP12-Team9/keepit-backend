package swyp12.team9.server.api.stat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "요일별 링크 저장 개수")
public record DayLinkCountResponse(
        @Schema(description = "요일", example = "월")
        String day,

        @Schema(description = "저장된 링크 개수", example = "10")
        Long linkCount
) {
    /**
     * DayLinkCountResponse 생성
     *
     * @param day       요일
     * @param linkCount 저장된 링크 개수
     * @return DayLinkCountResponse
     */
    public static DayLinkCountResponse of(String day, Long linkCount) {
        return DayLinkCountResponse.builder()
                .day(day)
                .linkCount(linkCount)
                .build();
    }
}
