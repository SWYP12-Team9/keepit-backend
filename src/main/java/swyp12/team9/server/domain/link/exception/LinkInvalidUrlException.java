package swyp12.team9.server.domain.link.exception;

import swyp12.team9.server.global.exception.BusinessException;
import swyp12.team9.server.global.exception.ErrorCode;

/**
 * URL 형식이 유효하지 않을 때 발생하는 예외
 */
public class LinkInvalidUrlException extends BusinessException {

    public LinkInvalidUrlException() {
        super(ErrorCode.LINK_INVALID_URL);
    }

    public LinkInvalidUrlException(String message) {
        super(ErrorCode.LINK_INVALID_URL, message);
    }
}