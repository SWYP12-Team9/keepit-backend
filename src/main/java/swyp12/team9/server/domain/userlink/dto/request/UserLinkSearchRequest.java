package swyp12.team9.server.domain.userlink.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 링크 검색 요청 DTO (커서 기반 페이징)
 * <p>
 * 검색 가능한 필드: - why: 저장 이유 - memo: 메모 - title: 링크 제목 - aiSummary: AI 요약 - url: 링크 주소
 */
@Schema(description = "링크 검색 요청")
public record UserLinkSearchRequest(

        @Schema(
                description = "검색 키워드 (2~50자)",
                example = "Spring Boot",
                minLength = 2,
                maxLength = 50
        )
        @Size(min = 2, max = 50, message = "검색어는 2~50자여야 합니다")
        String keyword,

        @Schema(
                description = """
                        검색 대상 필드 (선택)
                        - null 또는 빈값: 전체 필드 검색
                        - why: 저장 이유에서만 검색
                        - memo: 메모에서만 검색
                        - title: 제목에서만 검색
                        - aiSummary: AI 요약에서만 검색
                        - url: URL에서만 검색
                        """,
                example = "title",
                allowableValues = {"why", "memo", "title", "aiSummary", "url"}
        )
        @Pattern(regexp = "^(why|memo|title|aiSummary|url)$",
                message = "검색 필드는 why, memo, title, aiSummary, url 중 하나여야 합니다"
        )
        String field,

        @Schema(description = "커서 (첫 요청 시 null)", example = "10")
        String cursor,

        @Schema(description = "페이지 크기", example = "20", defaultValue = "20")
        @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다")
        @Max(value = 50, message = "페이지 크기는 50 이하여야 합니다")
        Integer size
) {
    /**
     * 기본값이 적용된 생성자
     */
    public UserLinkSearchRequest {
        if (size == null) {
            size = 20;
        }
    }

    /**
     * 키워드만으로 생성하는 팩토리 메서드
     */
    public static UserLinkSearchRequest of(String keyword) {
        return new UserLinkSearchRequest(keyword, null, null, 20);
    }

    /**
     * 키워드와 필드로 생성하는 팩토리 메서드
     */
    public static UserLinkSearchRequest of(String keyword, String field) {
        return new UserLinkSearchRequest(keyword, field, null, 20);
    }

    /**
     * 전체 파라미터로 생성하는 팩토리 메서드
     */
    public static UserLinkSearchRequest of(String keyword, String field, String cursor, int size) {
        return new UserLinkSearchRequest(keyword, field, cursor, size);
    }
}