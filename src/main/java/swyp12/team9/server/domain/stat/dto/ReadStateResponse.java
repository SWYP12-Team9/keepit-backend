package swyp12.team9.server.domain.stat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "읽음 상태 통계 응답")
public record ReadStateResponse(
        @Schema(description = "기준 날짜 (YYYY.MM.DD)", example = "2026.02.06")
        String date,

        @Schema(description = "읽은 링크 개수", example = "20")
        Long readLinkCount,

        @Schema(description = "안 읽은 링크 개수", example = "10")
        Long unreadLinkCount,

        @Schema(description = "읽은 링크 퍼센트 (정수)", example = "67")
        Integer readLinkPercent,

        @Schema(description = "안 읽은 링크 퍼센트 (정수)", example = "33")
        Integer unreadLinkPercent,

        @Schema(description = "인사이트 텍스트", example = "전체 링크의 33%를 아직 열람하지 않았어요. 미열람 링크는 '개발'에 총 5개로 가장 많아요.")
        String text
) {
    /**
     * ReadStateResponse 생성
     *
     * @param date              기준 날짜
     * @param readLinkCount     읽은 링크 개수
     * @param unreadLinkCount   안 읽은 링크 개수
     * @param readLinkPercent   읽은 링크 퍼센트
     * @param unreadLinkPercent 안 읽은 링크 퍼센트
     * @param text              인사이트 텍스트
     * @return ReadStateResponse
     */
    public static ReadStateResponse of(String date, Long readLinkCount, Long unreadLinkCount,
                                       Integer readLinkPercent, Integer unreadLinkPercent, String text) {
        return ReadStateResponse.builder()
                .date(date)
                .readLinkCount(readLinkCount)
                .unreadLinkCount(unreadLinkCount)
                .readLinkPercent(readLinkPercent)
                .unreadLinkPercent(unreadLinkPercent)
                .text(text)
                .build();
    }
}
