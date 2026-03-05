package swyp12.team9.server.global.infrastructure.storage.exception;

import swyp12.team9.server.global.exception.BusinessException;
import swyp12.team9.server.global.exception.ErrorCode;

public class FileDownloadFailedException extends BusinessException {

    public FileDownloadFailedException() {
        super(ErrorCode.FILE_DOWNLOAD_FAILED);
    }
}
