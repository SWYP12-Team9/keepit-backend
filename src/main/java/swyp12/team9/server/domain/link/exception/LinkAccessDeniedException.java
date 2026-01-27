package swyp12.team9.server.domain.link.exception;

import swyp12.team9.server.global.exception.BusinessException;
import swyp12.team9.server.global.exception.ErrorCode;

public class LinkAccessDeniedException extends BusinessException {

    public LinkAccessDeniedException(String message) {
        super(ErrorCode.LINK_ACCESS_DENIED, message);
    }
}
