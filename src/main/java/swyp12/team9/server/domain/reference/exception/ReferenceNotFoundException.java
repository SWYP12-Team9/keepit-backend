package swyp12.team9.server.domain.reference.exception;

import swyp12.team9.server.global.exception.BusinessException;
import swyp12.team9.server.global.exception.ErrorCode;

public class ReferenceNotFoundException extends BusinessException {


    public ReferenceNotFoundException() {
        super(ErrorCode.REFERENCE_NOT_FOUND);
    }

    public ReferenceNotFoundException(String message) {
        super(ErrorCode.REFERENCE_NOT_FOUND, message);
    }
}
