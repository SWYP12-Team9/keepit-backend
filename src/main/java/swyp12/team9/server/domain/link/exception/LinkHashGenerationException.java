package swyp12.team9.server.domain.link.exception;

import swyp12.team9.server.global.exception.BusinessException;
import swyp12.team9.server.global.exception.ErrorCode;

public class LinkHashGenerationException extends BusinessException {

    public LinkHashGenerationException(Throwable cause) {
        super(ErrorCode.LINK_HASH_GENERATION_FAILED, cause.getMessage());
    }

}
