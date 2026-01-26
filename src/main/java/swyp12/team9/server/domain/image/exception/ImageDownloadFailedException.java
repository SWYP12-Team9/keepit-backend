package swyp12.team9.server.domain.image.exception;

import swyp12.team9.server.global.exception.BusinessException;
import swyp12.team9.server.global.exception.ErrorCode;

public class ImageDownloadFailedException extends BusinessException {

    public ImageDownloadFailedException() {
        super(ErrorCode.IMAGE_DOWNLOAD_FAILED);
    }

    public ImageDownloadFailedException(String message) {
        super(ErrorCode.IMAGE_DOWNLOAD_FAILED, message);
    }
}