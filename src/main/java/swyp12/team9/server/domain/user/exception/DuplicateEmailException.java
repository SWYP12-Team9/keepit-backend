package swyp12.team9.server.domain.user.exception;

import swyp12.team9.server.global.exception.BusinessException;
import swyp12.team9.server.global.exception.ErrorCode;

public class DuplicateEmailException extends BusinessException {

    public DuplicateEmailException() {
        super(ErrorCode.EMAIL_DUPLICATION);
    }

    public DuplicateEmailException(String message) {
        super(ErrorCode.EMAIL_DUPLICATION, message);
    }
}