package swyp12.team9.server.domain.link.exception;

import swyp12.team9.server.global.exception.BusinessException;
import swyp12.team9.server.global.exception.ErrorCode;

public class LinkScrapingServerException extends BusinessException {

    public LinkScrapingServerException() {
        super(ErrorCode.LINK_SCRAPING_SERVER_ERROR);
    }
}