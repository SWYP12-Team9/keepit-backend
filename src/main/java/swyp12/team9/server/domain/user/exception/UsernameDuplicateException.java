package swyp12.team9.server.domain.user.exception;

import swyp12.team9.server.global.exception.BusinessException;
import swyp12.team9.server.global.exception.ErrorCode;

public class UsernameDuplicateException extends BusinessException {

    public UsernameDuplicateException() {
        super(ErrorCode.USERNAME_DUPLICATE);
    }

    public UsernameDuplicateException(String message) {
        super(ErrorCode.USERNAME_DUPLICATE, message);
    }
}