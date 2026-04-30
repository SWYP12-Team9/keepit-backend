package swyp12.team9.server.domain.userlink.event;

/**
 * UserLink 삭제 이벤트
 * - 추천 인덱스 삭제와 사용자 저장 링크 ID 캐시 무효화 트리거
 */
public record UserLinkDeletedEvent(Long userLinkId, Long userId) {

    public static UserLinkDeletedEvent of(Long userLinkId, Long userId) {
        return new UserLinkDeletedEvent(userLinkId, userId);
    }
}
