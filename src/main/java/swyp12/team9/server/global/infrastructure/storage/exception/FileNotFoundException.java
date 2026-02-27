package swyp12.team9.server.global.infrastructure.storage.exception;

import swyp12.team9.server.global.exception.BusinessException;
import swyp12.team9.server.global.exception.ErrorCode;

public class FileNotFoundException extends BusinessException {

    public FileNotFoundException() {
        super(ErrorCode.FILE_NOT_FOUND);
    }
}
