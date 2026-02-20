package swyp12.team9.server.domain.userlink.event;

/**
 * UserLink 수정 이벤트
 * - 재인덱싱 및 기타 후속 작업 트리거
 */
public record UserLinkUpdatedEvent(Long userLinkId) {

    public static UserLinkUpdatedEvent of(Long userLinkId) {
        return new UserLinkUpdatedEvent(userLinkId);
    }
}
