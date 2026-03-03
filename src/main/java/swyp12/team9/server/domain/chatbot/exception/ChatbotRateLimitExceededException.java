package swyp12.team9.server.domain.chatbot.exception;

import swyp12.team9.server.global.exception.BusinessException;
import swyp12.team9.server.global.exception.ErrorCode;

/**
 * 챗봇 요청 제한 초과 예외
 * - 하루 최대 요청 횟수를 초과했을 때 발생
 */
public class ChatbotRateLimitExceededException extends BusinessException {

    public ChatbotRateLimitExceededException() {
        super(ErrorCode.CHATBOT_RATE_LIMIT_EXCEEDED);
    }
}
