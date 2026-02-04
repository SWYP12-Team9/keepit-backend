package swyp12.team9.server.domain.user.exception;

import swyp12.team9.server.global.exception.BusinessException;
import swyp12.team9.server.global.exception.ErrorCode;

public class ProfileNotCompletedException extends BusinessException {

    public ProfileNotCompletedException() {
        super(ErrorCode.PROFILE_NOT_COMPLETED);
    }
}