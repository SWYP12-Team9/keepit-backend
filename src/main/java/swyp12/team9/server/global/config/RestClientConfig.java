package swyp12.team9.server.global.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import swyp12.team9.server.domain.link.exception.LinkScrapingServerException;
import swyp12.team9.server.global.interceptor.ScrapingClientInterceptor;

@Configuration
@Slf4j
public class RestClientConfig {

    @Value("${scraping.api.base-url:http://localhost:8000}")
    private String scrapingApiBaseUrl;

    @Bean
    public RestClient scrapingRestClient(RestClient.Builder builder, CloseableHttpClient httpClient) {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);

        return builder
                .baseUrl(scrapingApiBaseUrl)
                .requestFactory(new BufferingClientHttpRequestFactory(factory))
                .requestInterceptor(new ScrapingClientInterceptor())
                .defaultStatusHandler(HttpStatusCode::is4xxClientError, (request, response) -> {
                    log.warn("스크래핑 API 4xx 에러 - URI: {}, Status: {}", request.getURI(), response.getStatusCode());
                    throw new LinkScrapingServerException();
                })
                .defaultStatusHandler(HttpStatusCode::is5xxServerError, (request, response) -> {
                    log.error("스크래핑 API 5xx 에러 - URI: {}, Status: {}", request.getURI(), response.getStatusCode());
                    throw new LinkScrapingServerException();
                })
                .build();
    }

    /**
     * Apache HttpClient 설정
     * - Connection Pool
     * - Timeout
     * - Keep-Alive 활성화
     */
    @Bean
    public CloseableHttpClient httpClient() {
        // Connection Pool 설정
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(10))
                .setSocketTimeout(Timeout.ofSeconds(60))
                .build();

        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(30);  // 전체 최대 연결 수
        connectionManager.setDefaultMaxPerRoute(30);  // 라우트당 최대 연결 수
        connectionManager.setDefaultConnectionConfig(connectionConfig);

        // Request 설정
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofSeconds(30))  // Connection Pool에서 연결 대기 시간
                .setResponseTimeout(Timeout.ofSeconds(60))  // 응답 대기 시간
                .build();

        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .evictExpiredConnections()  // 만료된 연결 자동 제거
                .evictIdleConnections(Timeout.ofSeconds(15))  // 15초 이상 idle 연결 제거
                .build();
    }
}
