package swyp12.team9.server.domain.link.exception;

import swyp12.team9.server.global.exception.BusinessException;
import swyp12.team9.server.global.exception.ErrorCode;

public class LinkStreamInvalidMessageException extends BusinessException {

    public LinkStreamInvalidMessageException() {
        super(ErrorCode.LINK_STREAM_INVALID_MESSAGE);
    }
}
