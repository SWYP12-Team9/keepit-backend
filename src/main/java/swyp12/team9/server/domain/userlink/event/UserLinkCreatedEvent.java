package swyp12.team9.server.domain.userlink.event;

/**
 * UserLink 생성 이벤트
 * - 인덱싱 및 기타 후속 작업 트리거
 */
public record UserLinkCreatedEvent(Long userLinkId) {

    public static UserLinkCreatedEvent of(Long userLinkId) {
        return new UserLinkCreatedEvent(userLinkId);
    }
}
