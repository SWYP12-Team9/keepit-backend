package swyp12.team9.server.api.userlink.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collections;
import java.util.List;
import lombok.Builder;
import swyp12.team9.server.domain.userlink.model.UserLink;

/**
 * 링크 검색 결과 응답 DTO (커서 기반 페이징)
 * <p>
 * 검색 결과와 커서 페이징 정보를 함께 반환합니다.
 */
@Builder
@Schema(description = "링크 검색 결과 응답")
public record UserLinkSearchResponse(

        @Schema(description = "검색 결과 목록")
        List<UserLinkSearchItem> items,

        @Schema(description = "검색어", example = "Spring Boot")
        String keyword,

        @Schema(description = "다음 페이지 커서 (마지막 페이지면 null)", example = "10")
        String nextCursor,

        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext
) {

    /**
     * List<UserLink>로부터 검색 응답 생성 (커서 기반)
     *
     * @param userLinks 검색 결과 리스트 (size + 1개 조회됨)
     * @param keyword   검색에 사용된 키워드
     * @param size      요청된 페이지 크기
     * @return 검색 응답 DTO
     */
    public static UserLinkSearchResponse from(List<UserLink> userLinks, String keyword, int size) {
        boolean hasNext = userLinks.size() > size;

        // size+1개를 가져왔으므로, 실제 반환은 size개만
        List<UserLink> resultList = hasNext ? userLinks.subList(0, size) : userLinks;

        List<UserLinkSearchItem> items = resultList.stream()
                .map(userLink -> UserLinkSearchItem.from(userLink, keyword))
                .toList();

        // 다음 커서는 마지막 아이템의 ID
        String nextCursor = hasNext && !resultList.isEmpty()
                ? String.valueOf(resultList.get(resultList.size() - 1).getId())
                : null;

        return UserLinkSearchResponse.builder()
                .items(items)
                .keyword(keyword)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }

    /**
     * 빈 검색 결과 생성
     *
     * @return 빈 검색 응답 DTO
     */
    public static UserLinkSearchResponse empty() {
        return UserLinkSearchResponse.builder()
                .items(Collections.emptyList())
                .keyword("")
                .nextCursor(null)
                .hasNext(false)
                .build();
    }

    /**
     * 개별 검색 결과 아이템
     * <p>
     * 검색어 하이라이팅을 위해 매칭된 필드 정보를 포함합니다.
     */
    @Builder
    @Schema(description = "검색 결과 아이템")
    public record UserLinkSearchItem(

            @Schema(description = "사용자 링크 ID", example = "1")
            Long id,

            @Schema(description = "링크 제목", example = "Spring Boot 가이드")
            String title,

            @Schema(description = "링크 URL", example = "https://example.com/spring")
            String url,

            @Schema(description = "AI 요약", example = "Spring Boot에 대한 종합 가이드입니다.")
            String aiSummary,

            @Schema(description = "저장 이유", example = "개발 공부용")
            String why,

            @Schema(description = "메모", example = "핵심 내용 정리 필요")
            String memo,

            @Schema(description = "썸네일 이미지 URL", example = "https://example.com/thumb.jpg")
            String thumbnailUrl,

            @Schema(description = "조회수", example = "10")
            Long viewCount,

            @Schema(description = "검색어가 매칭된 필드 목록", example = "[\"title\", \"why\"]")
            List<String> matchedFields
    ) {

        /**
         * UserLink 엔티티로부터 검색 아이템 생성
         *
         * @param userLink UserLink 엔티티
         * @param keyword  검색 키워드 (매칭 필드 확인용)
         * @return 검색 아이템 DTO
         */
        public static UserLinkSearchItem from(UserLink userLink, String keyword) {
            // 검색어가 매칭된 필드들을 찾음 (하이라이팅용)
            List<String> matchedFields = findMatchedFields(userLink, keyword);

            return UserLinkSearchItem.builder()
                    .id(userLink.getId())
                    .title(userLink.getLink().getTitle())
                    .url(userLink.getLink().getUrl())
                    .aiSummary(userLink.getLink().getAiSummary())
                    .why(userLink.getWhy())
                    .memo(userLink.getMemo())
                    .thumbnailUrl(userLink.getLink().getPreviewImageUrl())
                    .viewCount(userLink.getViewCount())
                    .matchedFields(matchedFields)
                    .build();
        }

        /**
         * 검색어가 매칭된 필드들을 찾아 반환
         * <p>
         * 프론트엔드에서 검색어 하이라이팅에 활용할 수 있습니다.
         *
         * @param userLink UserLink 엔티티
         * @param keyword  검색 키워드
         * @return 매칭된 필드명 목록
         */
        private static List<String> findMatchedFields(UserLink userLink, String keyword) {
            if (keyword == null || keyword.isEmpty()) {
                return Collections.emptyList();
            }

            String lowerKeyword = keyword.toLowerCase();
            java.util.List<String> matched = new java.util.ArrayList<>();

            // 각 필드에서 검색어 매칭 확인
            if (containsIgnoreCase(userLink.getWhy(), lowerKeyword)) {
                matched.add("why");
            }
            if (containsIgnoreCase(userLink.getMemo(), lowerKeyword)) {
                matched.add("memo");
            }
            if (containsIgnoreCase(userLink.getLink().getTitle(), lowerKeyword)) {
                matched.add("title");
            }
            if (containsIgnoreCase(userLink.getLink().getAiSummary(), lowerKeyword)) {
                matched.add("aiSummary");
            }
            if (containsIgnoreCase(userLink.getLink().getUrl(), lowerKeyword)) {
                matched.add("url");
            }

            return matched;
        }

        /**
         * 대소문자 구분 없이 문자열 포함 여부 확인
         */
        private static boolean containsIgnoreCase(String text, String keyword) {
            return text != null && text.toLowerCase().contains(keyword);
        }
    }
}