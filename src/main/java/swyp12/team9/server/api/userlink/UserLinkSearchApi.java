package swyp12.team9.server.api.userlink;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import swyp12.team9.server.api.userlink.dto.response.UserLinkSearchResponse;
import swyp12.team9.server.global.annotation.ApiSpec;
import swyp12.team9.server.global.annotation.CurrentUserId;
import swyp12.team9.server.global.common.dto.ApiResponse;
import swyp12.team9.server.global.exception.ErrorCode;

/**
 * UserLinkSearch API 인터페이스 사용자 링크 검색 관련 API 스펙을 정의합니다.
 */
@Tag(name = "UserLink Search", description = "링크 검색 API - 홈, 레퍼런스 페이지")
@RequestMapping("/api/v1/user-links/search")
public interface UserLinkSearchApi {

    /**
     * 링크 검색 (홈/레퍼런스) 로그인한 사용자의 링크에서 검색합니다. referenceId 파라미터 유무에 따라 검색 범위가 달라집니다.
     */
    @Operation(
            summary = "링크 검색 (홈/레퍼런스)",
            description = """
                    키워드를 통해 링크를 검색합니다. (커서 기반 페이징)
                    
                    **검색 범위:**
                    - referenceId 없으면: 내 링크 전체에서 검색 (홈 페이지)
                    - referenceId 있으면: 해당 레퍼런스 폴더 내 링크에서 검색
                      (미지정 폴더도 referenceId로 검색 가능)
                    
                    **검색 대상 필드:**
                    - why: 저장 이유
                    - memo: 메모
                    - title: 링크 제목
                    - aiSummary: AI 요약
                    - url: 링크 URL
                    
                    **검색 방식:**
                    - field 파라미터가 없으면 모든 필드에서 검색
                    - field 파라미터가 있으면 해당 필드에서만 검색
                    """
    )
    @ApiSpec(
            status = HttpStatus.OK,
            errors = {
                    ErrorCode.VALIDATION_ERROR,
                    ErrorCode.UNAUTHORIZED,
                    ErrorCode.REFERENCE_NOT_FOUND,
                    ErrorCode.REFERENCE_ACCESS_DENIED
            }
    )
    @GetMapping
    ApiResponse<UserLinkSearchResponse> searchLinks(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @Parameter(description = "검색 키워드 (2~50자)", example = "좋은")
            @RequestParam @NotBlank @Size(min = 2, max = 50) String keyword,
            @Parameter(description = """
                    검색 대상 필드 (선택)
                    - 빈값: 전체 필드 검색
                    - why: 저장 이유에서만 검색
                    - memo: 메모에서만 검색
                    - title: 제목에서만 검색
                    - aiSummary: AI 요약에서만 검색
                    - url: URL에서만 검색
                    """,
                    example = "why"
            )
            @RequestParam(required = false) String field,
            @Parameter(description = "레퍼런스 폴더 ID (선택) - 지정 시 해당 폴더 내 링크에서만 검색 (미지정 폴더도 referenceId로 검색)", example = "1")
            @RequestParam(required = false) Long referenceId,
            @Parameter(description = "커서 (첫 요청 시 null)", example = "10")
            @RequestParam(required = false) String cursor,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size
    );

}