package swyp12.team9.server.domain.userlink.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 자동완성 응답 DTO
 *
 * 사용자가 입력 중인 텍스트에 대한 자동완성 제안을 반환합니다.
 *
 * @param suggestions 자동완성 제안 목록 (최대 10개)
 * @param query 원본 검색어
 * @param type 자동완성 타입 (title, domain)
 */
@Schema(description = "자동완성 응답")
public record AutocompleteResponse(

        @Schema(description = "자동완성 제안 목록", example = "[\"Spring Boot Tutorial\", \"Spring Security Guide\"]")
        List<String> suggestions,

        @Schema(description = "원본 검색어", example = "Spring")
        String query,

        @Schema(description = "자동완성 타입", example = "title")
        String type
) {
    /**
     * 제목 자동완성 응답 생성
     */
    public static AutocompleteResponse ofTitles(List<String> suggestions, String query) {
        return new AutocompleteResponse(suggestions, query, "title");
    }

    /**
     * 도메인 자동완성 응답 생성
     */
    public static AutocompleteResponse ofDomains(List<String> suggestions, String query) {
        return new AutocompleteResponse(suggestions, query, "domain");
    }

    /**
     * 빈 응답 생성
     */
    public static AutocompleteResponse empty(String query, String type) {
        return new AutocompleteResponse(List.of(), query, type);
    }
}