package swyp12.team9.server.domain.userlink.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import lombok.Builder;
import swyp12.team9.server.domain.reference.model.Reference;
import swyp12.team9.server.domain.userlink.model.LinkStatus;
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
        List<UserLinkSearchContent> contents,

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
     * @param userLinks         검색 결과 리스트 (size + 1개 조회됨)
     * @param keyword           검색에 사용된 키워드
     * @param size              요청된 페이지 크기
     * @param referenceResolver UserLink ID로 Reference를 조회하는 함수
     * @return 검색 응답 DTO
     */
    public static UserLinkSearchResponse from(List<UserLink> userLinks, String keyword, int size,
                                              Function<Long, Reference> referenceResolver) {
        boolean hasNext = userLinks.size() > size;

        // size+1개를 가져왔으므로, 실제 반환은 size개만
        List<UserLink> resultList = hasNext ? userLinks.subList(0, size) : userLinks;

        List<UserLinkSearchContent> contents = resultList.stream()
                .map(userLink -> {
                    Reference reference = referenceResolver.apply(userLink.getId());
                    return UserLinkSearchContent.from(userLink, reference, keyword);
                })
                .toList();

        // 다음 커서는 마지막 아이템의 ID
        String nextCursor = hasNext && !resultList.isEmpty()
                ? String.valueOf(resultList.getLast().getId())
                : null;

        return UserLinkSearchResponse.builder()
                .contents(contents)
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
                .contents(Collections.emptyList())
                .keyword("")
                .nextCursor(null)
                .hasNext(false)
                .build();
    }

    /**
     * 개별 검색 결과 내용
     * <p>
     * UserLinkListResponse와 동일한 구조 + 검색어 하이라이팅용 matchedFields
     */
    @Builder
    @Schema(description = "검색 결과 아이템")
    public record UserLinkSearchContent(

            @Schema(description = "사용자 링크 ID", example = "1")
            Long id,

            @Schema(description = "레퍼런스 정보")
            ReferenceInfo reference,

            @Schema(description = "링크 제목", example = "Spring Boot 가이드")
            String title,

            @Schema(description = "링크 URL", example = "https://example.com/spring")
            String url,

            @Schema(description = "AI 요약", example = "Spring Boot에 대한 종합 가이드입니다.")
            String aiSummary,

            @Schema(description = "읽음 상태", example = "UNREAD")
            LinkStatus status,

            @Schema(description = "조회수", example = "10")
            Long viewCount,

            @Schema(description = "검색어가 매칭된 필드 목록", example = "[\"title\", \"why\"]")
            List<String> matchedFields
    ) {

        /**
         * UserLink 엔티티로부터 검색 아이템 생성
         *
         * @param userLink  UserLink 엔티티
         * @param reference Reference 엔티티
         * @param keyword   검색 키워드 (매칭 필드 확인용)
         * @return 검색 아이템 DTO
         */
        public static UserLinkSearchContent from(UserLink userLink, Reference reference, String keyword) {
            List<String> matchedFields = findMatchedFields(userLink, keyword);
            ReferenceInfo referenceInfo = ReferenceInfo.from(reference);

            return UserLinkSearchContent.builder()
                    .id(userLink.getId())
                    .reference(referenceInfo)
                    .title(userLink.getLink().getTitle())
                    .url(userLink.getLink().getUrl())
                    .aiSummary(userLink.getLink().getAiSummary())
                    .status(userLink.getStatus())
                    .viewCount(userLink.getViewCount())
                    .matchedFields(matchedFields)
                    .build();
        }

        /**
         * 검색어가 매칭된 필드들을 찾아 반환
         */
        private static List<String> findMatchedFields(UserLink userLink, String keyword) {
            if (keyword == null || keyword.isEmpty()) {
                return Collections.emptyList();
            }

            String lowerKeyword = keyword.toLowerCase();
            java.util.List<String> matched = new java.util.ArrayList<>();

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

        private static boolean containsIgnoreCase(String text, String keyword) {
            return text != null && text.toLowerCase().contains(keyword);
        }
    }
}