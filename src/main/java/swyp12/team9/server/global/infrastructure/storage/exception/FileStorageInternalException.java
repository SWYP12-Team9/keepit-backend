package swyp12.team9.server.global.infrastructure.storage.exception;

import swyp12.team9.server.global.exception.BusinessException;
import swyp12.team9.server.global.exception.ErrorCode;

public class FileStorageInternalException extends BusinessException {

    public FileStorageInternalException() {
        super(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
