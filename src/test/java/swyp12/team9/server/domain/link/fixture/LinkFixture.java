package swyp12.team9.server.domain.link.fixture;

import swyp12.team9.server.domain.link.dto.ScrapingResponse;
import swyp12.team9.server.domain.link.model.Link;

public class LinkFixture {

    public static final String URL = "https://example.com";
    public static final String TITLE = "테스트 링크 제목";
    public static final String DESCRIPTION = "테스트 링크 설명";
    public static final String FAVICON_URL = "https://example.com/favicon.ico";
    public static final String CONTENT = "링크 컨텐츠입니다.";
    public static final String AI_SUMMARY = "AI가 생성한 요약 내용입니다.";

    /**
     * 기본 Link 엔티티 생성
     */
    public static Link createLink() {
        return Link.create(URL, TITLE, DESCRIPTION, FAVICON_URL, CONTENT);
    }

    /**
     * ID가 설정된 Link 생성 (테스트용)
     * 단위 테스트에서 Mock Repository의 반환값으로 사용
     */
    public static Link createLinkWithId(Long id) {
        Link link = Link.create(URL, TITLE, DESCRIPTION, FAVICON_URL, CONTENT);
        setId(link, id);
        return link;
    }

    /**
     * 커스텀 URL로 Link 생성
     */
    public static Link createLinkWithUrl(String url) {
        return Link.create(url, TITLE, DESCRIPTION, FAVICON_URL, CONTENT);
    }

    /**
     * 커스텀 URL과 ID로 Link 생성
     */
    public static Link createLinkWithUrlAndId(Long id, String url) {
        Link link = Link.create(url, TITLE, DESCRIPTION, FAVICON_URL, CONTENT);
        setId(link, id);
        return link;
    }

    /**
     * Reflection을 사용하여 Link 엔티티에 ID 설정
     * JPA가 자동으로 생성하는 ID를 테스트에서 시뮬레이션하기 위해 사용
     */
    private static void setId(Link link, Long id) {
        try {
            java.lang.reflect.Field idField = Link.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(link, id);
        } catch (Exception e) {
            throw new RuntimeException("ID 설정 실패", e);
        }
    }

    /**
     * AI 요약이 포함된 Link 생성
     */
    public static Link createLinkWithAiSummary() {
        Link link = createLink();
        link.updateAiSummary(AI_SUMMARY);
        return link;
    }

    /**
     * 스크래핑 응답 생성 (정상 케이스)
     */
    public static ScrapingResponse createScrapingResponse() {
        return new ScrapingResponse(
                true,
                TITLE,
                DESCRIPTION,
                FAVICON_URL,
                URL,
                CONTENT
        );
    }

    /**
     * 스크래핑 응답 생성 (커스텀 URL)
     */
    public static ScrapingResponse createScrapingResponse(String url) {
        return new ScrapingResponse(
                true,
                TITLE,
                DESCRIPTION,
                FAVICON_URL,
                url,
                CONTENT
        );
    }

    /**
     * 스크래핑 응답 생성 (최소 정보만 - description, content 없음)
     */
    public static ScrapingResponse createMinimalScrapingResponse(String url) {
        return new ScrapingResponse(
                true,
                TITLE,
                null,
                FAVICON_URL,
                url,
                null
        );
    }

    /**
     * 스크래핑 응답 생성 (빈 제목)
     */
    public static ScrapingResponse createScrapingResponseWithEmptyTitle(String url) {
        return new ScrapingResponse(
                true,
                "",
                DESCRIPTION,
                FAVICON_URL,
                url,
                CONTENT
        );
    }
}