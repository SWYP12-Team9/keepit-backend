package swyp12.team9.server.domain.image.exception;

import swyp12.team9.server.global.exception.BusinessException;
import swyp12.team9.server.global.exception.ErrorCode;

public class ImageUploadFailedException extends BusinessException {

    public ImageUploadFailedException() {
        super(ErrorCode.IMAGE_UPLOAD_FAILED);
    }

}