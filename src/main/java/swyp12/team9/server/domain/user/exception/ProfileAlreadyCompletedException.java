package swyp12.team9.server.domain.user.exception;

import swyp12.team9.server.global.exception.BusinessException;
import swyp12.team9.server.global.exception.ErrorCode;

public class ProfileAlreadyCompletedException extends BusinessException {

    public ProfileAlreadyCompletedException() {
        super(ErrorCode.PROFILE_ALREADY_COMPLETED);
    }

    public ProfileAlreadyCompletedException(String message) {
        super(ErrorCode.PROFILE_ALREADY_COMPLETED, message);
    }
}