package swyp12.team9.server.domain.reference.exception;

import swyp12.team9.server.global.exception.BusinessException;
import swyp12.team9.server.global.exception.ErrorCode;

public class ReferenceValidationException extends BusinessException {
    public ReferenceValidationException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ReferenceValidationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
