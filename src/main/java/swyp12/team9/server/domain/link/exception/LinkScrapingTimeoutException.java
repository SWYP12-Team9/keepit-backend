package swyp12.team9.server.domain.link.exception;

import swyp12.team9.server.global.exception.BusinessException;
import swyp12.team9.server.global.exception.ErrorCode;

public class LinkScrapingTimeoutException extends BusinessException {

    public LinkScrapingTimeoutException() {
        super(ErrorCode.LINK_SCRAPING_TIMEOUT);
    }
}