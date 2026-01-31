package swyp12.team9.server.domain.reference.exception;

import swyp12.team9.server.global.exception.BusinessException;
import swyp12.team9.server.global.exception.ErrorCode;

public class ReferenceTitleDuplicateException extends BusinessException {

    public ReferenceTitleDuplicateException() {
        super(ErrorCode.REFERENCE_TITLE_DUPLICATE);
    }

    public ReferenceTitleDuplicateException(String message) {
        super(ErrorCode.REFERENCE_TITLE_DUPLICATE, message);
    }
}