package swyp12.team9.server.domain.terms.exception;

import swyp12.team9.server.global.exception.BusinessException;
import swyp12.team9.server.global.exception.ErrorCode;

public class TermsFileReadException extends BusinessException {

    public TermsFileReadException() {
        super(ErrorCode.TERMS_FILE_READ_ERROR);
    }

    public TermsFileReadException(String message) {
        super(ErrorCode.TERMS_FILE_READ_ERROR, message);
    }
}
