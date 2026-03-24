package swyp12.team9.server.domain.link.exception;

import swyp12.team9.server.global.exception.BusinessException;
import swyp12.team9.server.global.exception.ErrorCode;

public class LinkProcessingException extends BusinessException {

    public LinkProcessingException() {
        super(ErrorCode.LINK_PROCESSING_FAILED);
    }
}
