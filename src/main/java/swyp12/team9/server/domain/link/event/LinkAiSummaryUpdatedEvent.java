package swyp12.team9.server.domain.link.event;

/**
 * Link AI 요약 완료 이벤트
 * - AI 요약 생성 완료 후, 해당 Link를 사용하는 UserLink들의 챗봇 인덱싱 트리거
 */
public record LinkAiSummaryUpdatedEvent(Long linkId) {

    public static LinkAiSummaryUpdatedEvent of(Long linkId) {
        return new LinkAiSummaryUpdatedEvent(linkId);
    }
}
