package swyp12.team9.server.domain.userlink.exception;

import swyp12.team9.server.global.exception.BusinessException;
import swyp12.team9.server.global.exception.ErrorCode;

public class UserLinkDuplicateException extends BusinessException {

    public UserLinkDuplicateException() {
        super(ErrorCode.USER_LINK_DUPLICATE);
    }

}