package swyp12.team9.server.domain.userlink.exception;

import swyp12.team9.server.global.exception.BusinessException;
import swyp12.team9.server.global.exception.ErrorCode;

public class UserLinkAccessDeniedException extends BusinessException {

    public UserLinkAccessDeniedException() {
        super(ErrorCode.USER_LINK_ACCESS_DENIED);
    }

    public UserLinkAccessDeniedException(String message) {
        super(ErrorCode.USER_LINK_ACCESS_DENIED, message);
    }
}
