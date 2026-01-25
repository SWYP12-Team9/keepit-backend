package swyp12.team9.server.domain.scraper.exception;

/**
 * 스크래핑 실패 시 발생하는 예외
 */
public class ScrapingException extends RuntimeException {

  public ScrapingException(String message) {
    super(message);
  }

  public ScrapingException(String message, Throwable cause) {
    super(message, cause);
  }
}
