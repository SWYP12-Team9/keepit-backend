package swyp12.team9.server.domain.link.event;

public record LinkFailedEvent(Long linkId, Long userId, String reason) {

    public static LinkFailedEvent of(Long linkId, Long userId, String reason) {
        return new LinkFailedEvent(linkId, userId, reason);
    }
}
