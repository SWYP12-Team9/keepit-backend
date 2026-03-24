package swyp12.team9.server.global.infrastructure.storage.exception;

import swyp12.team9.server.global.exception.BusinessException;
import swyp12.team9.server.global.exception.ErrorCode;

public class FileDeleteFailedException extends BusinessException {

    public FileDeleteFailedException() {
        super(ErrorCode.FILE_DELETE_FAILED);
    }
}
