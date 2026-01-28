package swyp12.team9.server.domain.user.exception;

import swyp12.team9.server.global.exception.BusinessException;
import swyp12.team9.server.global.exception.ErrorCode;

public class NicknameDuplicateException extends BusinessException {

    public NicknameDuplicateException() {
        super(ErrorCode.NICKNAME_DUPLICATE);
    }

    public NicknameDuplicateException(String message) {
        super(ErrorCode.NICKNAME_DUPLICATE, message);
    }
}