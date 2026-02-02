package swyp12.team9.server.domain.reference.exception;

import swyp12.team9.server.global.exception.BusinessException;
import swyp12.team9.server.global.exception.ErrorCode;

public class ReferenceAccessDeniedException extends BusinessException {

    public ReferenceAccessDeniedException() {
        super(ErrorCode.REFERENCE_ACCESS_DENIED);
    }

}