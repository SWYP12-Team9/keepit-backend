package swyp12.team9.server.domain.chatbot.exception;

import lombok.Getter;
import swyp12.team9.server.global.exception.BusinessException;
import swyp12.team9.server.global.exception.ErrorCode;

/**
 * 챗봇 요청 제한 초과 예외
 * - 하루 최대 요청 횟수를 초과했을 때 발생
 */
@Getter
public class ChatbotRateLimitExceededException extends BusinessException {

    private final Long userId;

    public ChatbotRateLimitExceededException(Long userId) {
        super(ErrorCode.CHATBOT_RATE_LIMIT_EXCEEDED);
        this.userId = userId;
    }
}
