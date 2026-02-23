package swyp12.team9.server.domain.userlink.event;

/**
 * UserLink 삭제 이벤트
 * - 인덱스 삭제 및 기타 후속 작업 트리거
 */
public record UserLinkDeletedEvent(Long userLinkId) {

    public static UserLinkDeletedEvent of(Long userLinkId) {
        return new UserLinkDeletedEvent(userLinkId);
    }
}
