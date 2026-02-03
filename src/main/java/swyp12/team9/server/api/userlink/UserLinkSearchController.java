package swyp12.team9.server.api.userlink;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import swyp12.team9.server.api.userlink.dto.response.UserLinkSearchResponse;
import swyp12.team9.server.domain.userlink.service.search.UserLinkSearchService;
import swyp12.team9.server.global.annotation.CurrentUserId;
import swyp12.team9.server.global.common.dto.ApiResponse;

/**
 * 링크 검색 API 컨트롤러
 * <p>
 * 검색 단계별 구현: - 1단계 (현재): DB LIKE 검색 - MVP용, 빠른 구현 - 2단계: MySQL Full-Text Index - 성능 개선 - 3단계: Elasticsearch - 고급 검색 기능
 * <p>
 * 검색 대상 필드: - UserLink.why (저장 이유) - UserLink.memo (메모) - Link.title (링크 제목) - Link.aiSummary (AI 요약) - Link.url (링크
 * URL)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/user-links/search")
@RequiredArgsConstructor
public class UserLinkSearchController implements UserLinkSearchApi {

    private final UserLinkSearchService userLinkSearchService;

    /**
     * 링크 검색 (홈/레퍼런스)
     * - isDefault=true: 미지정 폴더 내 검색
     * - referenceId가 있으면: 해당 레퍼런스 폴더 내 검색
     * - 둘 다 없으면: 내 링크 전체에서 검색
     */
    @Override
    public ApiResponse<UserLinkSearchResponse> searchLinks(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @RequestParam String keyword,
            @RequestParam(required = false) String field,
            @RequestParam(required = false) Long referenceId,
            @RequestParam(required = false) Boolean isDefault,
            @RequestParam(required = false) String cursor,
            @RequestParam int size
    ) {
        UserLinkSearchResponse response;

        if (Boolean.TRUE.equals(isDefault)) {
            // 미지정 폴더 내 검색
            log.info("미지정 폴더 내 링크 검색 요청 - userId: {}, keyword: {}, field: {}, cursor: {}",
                    userId, keyword, field, cursor);
            response = userLinkSearchService.searchDefaultReference(userId, keyword, field, cursor, size);
        } else if (referenceId != null) {
            // 특정 레퍼런스 폴더 내 검색
            log.info("레퍼런스 폴더 내 링크 검색 요청 - userId: {}, referenceId: {}, keyword: {}, field: {}, cursor: {}",
                    userId, referenceId, keyword, field, cursor);
            response = userLinkSearchService.searchByReference(userId, referenceId, keyword, field, cursor, size);
        } else {
            // 내 링크 전체에서 검색
            log.info("내 링크 검색 요청 - userId: {}, keyword: {}, field: {}, cursor: {}", userId, keyword, field, cursor);
            response = userLinkSearchService.searchMyLinks(userId, keyword, field, cursor, size);
        }

        return ApiResponse.ok(response);
    }

    // 자동완성 API

//    /**
//     * 제목 자동완성
//     * <p>
//     * 사용자가 입력 중인 텍스트를 기반으로 제목을 자동완성합니다.
//     */
//    @Override
//    public ApiResponse<AutocompleteResponse> autocompleteTitles(
//            @Parameter(hidden = true) @CurrentUserId Long userId,
//            @Parameter(description = "입력 중인 텍스트", required = true, example = "Spring")
//            @RequestParam String query,
//            @Parameter(description = "검색 모드 (prefix: 접두사 매칭, contains: 부분 문자열)", example = "prefix")
//            @RequestParam(defaultValue = "prefix") String mode
//    ) {
//        log.debug("제목 자동완성 요청 - userId: {}, query: {}, mode: {}", userId, query, mode);
//
//        List<String> suggestions;
//        if ("contains".equals(mode)) {
//            suggestions = fullTextSearchService.autocompleteTitlesContaining(userId, query);
//        } else {
//            suggestions = fullTextSearchService.autocompleteTitles(userId, query);
//        }
//
//        return ApiResponse.ok(
//                AutocompleteResponse.ofTitles(suggestions, query)
//        );
//    }
//
//    /**
//     * 도메인 자동완성
//     * <p>
//     * 저장된 링크의 도메인을 자동완성합니다.
//     */
//    @Override
//    public ApiResponse<AutocompleteResponse> autocompleteDomains(
//            @Parameter(hidden = true) @CurrentUserId Long userId,
//            @Parameter(description = "입력 중인 도메인 텍스트", required = true, example = "github")
//            @RequestParam String query
//    ) {
//        log.debug("도메인 자동완성 요청 - userId: {}, query: {}", userId, query);
//
//        List<String> suggestions = fullTextSearchService.autocompleteDomains(userId, query);
//
//        return ApiResponse.ok(
//                AutocompleteResponse.ofDomains(suggestions, query)
//        );
//    }
}