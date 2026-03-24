package swyp12.team9.server.domain.link.exception;

import swyp12.team9.server.global.exception.BusinessException;
import swyp12.team9.server.global.exception.ErrorCode;

public class LinkStreamRetryLimitExceededException extends BusinessException {

    public LinkStreamRetryLimitExceededException() {
        super(ErrorCode.LINK_STREAM_RETRY_LIMIT_EXCEEDED);
    }
}
