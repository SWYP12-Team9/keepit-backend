package swyp12.team9.server.domain.terms.exception;

import swyp12.team9.server.global.exception.BusinessException;
import swyp12.team9.server.global.exception.ErrorCode;

public class InvalidTermsTypeException extends BusinessException {

    public InvalidTermsTypeException() {
        super(ErrorCode.INVALID_TYPE_VALUE);
    }
}
