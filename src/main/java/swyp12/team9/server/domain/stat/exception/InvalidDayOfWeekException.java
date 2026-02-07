package swyp12.team9.server.domain.stat.exception;

import swyp12.team9.server.global.exception.BusinessException;
import swyp12.team9.server.global.exception.ErrorCode;

/**
 * 유효하지 않은 요일 예외
 */
public class InvalidDayOfWeekException extends BusinessException {
    public InvalidDayOfWeekException() {
        super(ErrorCode.INVALID_INPUT_VALUE);
    }
}