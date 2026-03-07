package swyp12.team9.server.domain.link.event;

/**
 * Link 생성 이벤트
 * - AI 요약 등 Link 엔티티 대상 후속 작업 트리거
 */
public record LinkCreatedEvent(Long linkId, Long userId) {

    public static LinkCreatedEvent of(Long linkId, Long userId) {
        return new LinkCreatedEvent(linkId, userId);
    }
}
