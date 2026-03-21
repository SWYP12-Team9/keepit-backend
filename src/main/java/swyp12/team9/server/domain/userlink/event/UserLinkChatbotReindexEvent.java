package swyp12.team9.server.domain.userlink.event;

/**
 * UserLink의 챗봇 인덱스 재색인이 필요한 수정 이벤트
 */
public record UserLinkChatbotReindexEvent(Long userLinkId) {

    public static UserLinkChatbotReindexEvent of(Long userLinkId) {
        return new UserLinkChatbotReindexEvent(userLinkId);
    }
}
