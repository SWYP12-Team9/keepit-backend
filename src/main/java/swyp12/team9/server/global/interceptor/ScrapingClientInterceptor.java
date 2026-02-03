package swyp12.team9.server.global.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import swyp12.team9.server.domain.link.exception.LinkScrapingTimeoutException;

import java.io.IOException;
import java.net.SocketTimeoutException;

/**
 * 스크래핑 RestClient 인터셉터
 * 요청/응답 로깅 및 타임아웃 예외 처리를 담당합니다.
 */
@Slf4j
public class ScrapingClientInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution
    ) throws IOException {
        log.debug("스크래핑 API 요청 - Method: {}, URI: {}", request.getMethod(), request.getURI());

        try {
            ClientHttpResponse response = execution.execute(request, body);
            log.debug("스크래핑 API 응답 - Status: {}", response.getStatusCode());
            return response;
        } catch (SocketTimeoutException e) {
            log.error("스크래핑 API 타임아웃 - URI: {}", request.getURI());
            throw new LinkScrapingTimeoutException();
        } catch (IOException e) {
            // 타임아웃 관련 메시지 체크
            if (e.getMessage() != null && (e.getMessage().contains("timeout") || e.getMessage().contains("timed out"))) {
                log.error("스크래핑 API 타임아웃 - URI: {}", request.getURI());
                throw new LinkScrapingTimeoutException();
            }
            throw e;
        }
    }
}
