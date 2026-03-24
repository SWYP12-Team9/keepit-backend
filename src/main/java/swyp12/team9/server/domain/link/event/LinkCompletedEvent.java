package swyp12.team9.server.domain.link.event;

public record LinkCompletedEvent(Long linkId, String title, Long userId) {

    public static LinkCompletedEvent of(Long linkId, String title, Long userId) {
        return new LinkCompletedEvent(linkId, title, userId);
    }
}
