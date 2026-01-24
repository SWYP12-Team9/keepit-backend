package swyp12.team9.server.domain.link.exception;

import swyp12.team9.server.global.exception.BusinessException;
import swyp12.team9.server.global.exception.ErrorCode;

public class LinkNotFoundException extends BusinessException {

  public LinkNotFoundException() {
    super(ErrorCode.LINK_NOT_FOUND);
  }

  public LinkNotFoundException(String message) {
    super(ErrorCode.LINK_NOT_FOUND, message);
  }
}
