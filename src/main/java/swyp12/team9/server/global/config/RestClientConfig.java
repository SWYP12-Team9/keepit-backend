package swyp12.team9.server.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import swyp12.team9.server.domain.link.exception.LinkInvalidUrlException;
import swyp12.team9.server.domain.link.exception.LinkScrapingServerException;
import swyp12.team9.server.global.interceptor.ScrapingClientInterceptor;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Value("${scraping.api.base-url:http://localhost:8000}")
    private String scrapingApiBaseUrl;

    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        return builder.build();
    }

    @Bean
    public RestClient scrapingRestClient(RestClient.Builder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(30));

        return builder
                .baseUrl(scrapingApiBaseUrl)
                .requestFactory(new BufferingClientHttpRequestFactory(factory))
                .requestInterceptor(new ScrapingClientInterceptor())
                .defaultStatusHandler(HttpStatusCode::is4xxClientError, (request, response) -> {
                    throw new LinkInvalidUrlException();
                })
                .defaultStatusHandler(HttpStatusCode::is5xxServerError, (request, response) -> {
                    throw new LinkScrapingServerException();
                })
                .build();
    }
}
