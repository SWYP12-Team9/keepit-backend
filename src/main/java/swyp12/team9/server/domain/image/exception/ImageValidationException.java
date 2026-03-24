package swyp12.team9.server.domain.image.exception;

import swyp12.team9.server.global.exception.BusinessException;
import swyp12.team9.server.global.exception.ErrorCode;

public class ImageValidationException extends BusinessException {

    public ImageValidationException(ErrorCode errorCode) {
        super(errorCode);
    }
}