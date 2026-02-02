package swyp12.team9.server.domain.userlink.exception;

import swyp12.team9.server.global.exception.BusinessException;
import swyp12.team9.server.global.exception.ErrorCode;

public class ReferenceSelectionDuplicateException extends BusinessException {

    public ReferenceSelectionDuplicateException() {
        super(ErrorCode.REFERENCE_SELECTION_DUPLICATE);
    }

    public ReferenceSelectionDuplicateException(String message) {
        super(ErrorCode.REFERENCE_SELECTION_DUPLICATE, message);
    }
}
