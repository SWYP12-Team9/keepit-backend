package swyp12.team9.server.api.userlink;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import swyp12.team9.server.api.userlink.dto.response.UserLinkSearchResponse;
import swyp12.team9.server.domain.userlink.service.UserLinkSearchService;
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
     * - referenceId가 있으면: 해당 레퍼런스 폴더 내 검색 (미지정 폴더도 referenceId로 검색)
     * - referenceId가 없으면: 내 링크 전체에서 검색
     */
    @Override
    public ApiResponse<UserLinkSearchResponse> searchLinks(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @RequestParam String keyword,
            @RequestParam(required = false) String field,
            @RequestParam(required = false) Long referenceId,
            @RequestParam(required = false) String cursor,
            @RequestParam int size
    ) {
        UserLinkSearchResponse response;

        if (referenceId != null) {
            // 특정 레퍼런스 폴더 내 검색 (미지정 폴더 포함)
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
}