package swyp12.team9.server.domain.image.exception;

import swyp12.team9.server.global.exception.BusinessException;
import swyp12.team9.server.global.exception.ErrorCode;

public class ImageNotFoundException extends BusinessException {

    public ImageNotFoundException() {
        super(ErrorCode.IMAGE_NOT_FOUND);
    }

    public ImageNotFoundException(String message) {
        super(ErrorCode.IMAGE_NOT_FOUND, message);
    }
}