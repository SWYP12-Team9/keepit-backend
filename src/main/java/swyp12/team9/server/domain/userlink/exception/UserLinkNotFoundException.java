package swyp12.team9.server.domain.userlink.exception;

import swyp12.team9.server.global.exception.BusinessException;
import swyp12.team9.server.global.exception.ErrorCode;

public class UserLinkNotFoundException extends BusinessException {

    public UserLinkNotFoundException() {
        super(ErrorCode.USER_LINK_NOT_FOUND);
    }

    public UserLinkNotFoundException(String message) {
        super(ErrorCode.USER_LINK_NOT_FOUND, message);
    }
}
