package swyp12.team9.server.domain.terms.exception;

import swyp12.team9.server.global.exception.BusinessException;
import swyp12.team9.server.global.exception.ErrorCode;

public class TermsNotFoundException extends BusinessException {

    public TermsNotFoundException() {
        super(ErrorCode.TERMS_NOT_FOUND);
    }
}
