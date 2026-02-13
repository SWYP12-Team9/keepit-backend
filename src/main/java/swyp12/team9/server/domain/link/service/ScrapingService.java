package swyp12.team9.server.domain.link.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import swyp12.team9.server.domain.link.dto.ScrapingRequest;
import swyp12.team9.server.domain.link.dto.ScrapingResponse;
import swyp12.team9.server.domain.link.exception.LinkScrapingServerException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScrapingService {

    private final RestClient scrapingRestClient;

    /**
     * 외부 스크래핑 API를 호출하여 URL의 메타데이터를 가져옵니다.
     *
     * @param url        스크래핑할 URL
     * @param maxLength  content_preview 최대 길이
     * @return 스크래핑 결과
     */
    public ScrapingResponse scrapeUrl(String url, Integer maxLength) {
        log.info("스크래핑 API 호출 - URL: {}", url);

        ScrapingRequest request = ScrapingRequest.of(
                url,
                maxLength != null ? maxLength : 500
        );

        ScrapingResponse response = scrapingRestClient.post()
                .uri("/api/v1/scrape")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(ScrapingResponse.class); // 외부 서버 응답 대기 (수 초)

        if (response == null) {
            log.error("스크래핑 응답이 비어있음 - URL: {}", url);
            throw new LinkScrapingServerException();
        }

        log.info("스크래핑 완료 - URL: {}", url);
        return response;
    }
}