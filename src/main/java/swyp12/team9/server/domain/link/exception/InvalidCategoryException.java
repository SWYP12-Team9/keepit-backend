package swyp12.team9.server.domain.link.exception;

import swyp12.team9.server.global.exception.BusinessException;
import swyp12.team9.server.global.exception.ErrorCode;

public class InvalidCategoryException extends BusinessException {

    public InvalidCategoryException() {
        super(ErrorCode.INVALID_CATEGORY);
    }

    public InvalidCategoryException(String message) {
        super(ErrorCode.INVALID_CATEGORY, message);
    }
}
