package swyp12.team9.server.api.userlink;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import swyp12.team9.server.api.userlink.dto.request.UserLinkCreateRequest;
import swyp12.team9.server.api.userlink.dto.request.UserLinkUpdateRequest;
import swyp12.team9.server.api.userlink.dto.response.UserLinkListResponse;
import swyp12.team9.server.api.userlink.dto.response.UserLinkResponse;
import swyp12.team9.server.global.annotation.ApiSpec;
import swyp12.team9.server.global.annotation.CurrentUserId;
import swyp12.team9.server.global.common.dto.ApiResponse;
import swyp12.team9.server.global.exception.ErrorCode;
import swyp12.team9.server.global.util.PaginationUtils;

/**
 * UserLink API 인터페이스 사용자 링크 관련 API 스펙을 정의합니다.
 */
@Tag(name = "UserLink", description = "사용자 링크 관리 API")
@RequestMapping("/api/v1/user-links")
public interface UserLinkApi {
    @Operation(
            summary = "링크 게시물 생성",
            description = """
                    새로운 링크를 저장합니다.
                    
                    **레퍼런스 폴더 지정 방법**
                    1. 기존 폴더 선택: `referenceId`에 폴더 ID 전달
                    2. 새 폴더 생성: `newReference`에 제목과 색상 코드 전달
                    3. 미지정: 둘 다 null/빈 값이면 '미지정' 폴더로 자동 분류
                    
                    ⚠️ `referenceId`와 `newReference`는 동시에 사용할 수 없습니다.
                    
                    **URL 처리:**
                    - 기존 Link가 있으면 재사용
                    - 없으면 새로 생성 (추후 스크래핑 적용 예정)
                    """
    )
    @ApiSpec(
            status = HttpStatus.CREATED,
            errors = {
                    ErrorCode.VALIDATION_ERROR,
                    ErrorCode.UNAUTHORIZED,
                    ErrorCode.USER_NOT_FOUND,
                    ErrorCode.REFERENCE_NOT_FOUND,
                    ErrorCode.REFERENCE_TITLE_DUPLICATE,
                    ErrorCode.REFERENCE_SELECTION_DUPLICATE,
                    ErrorCode.LINK_INVALID_URL,
                    ErrorCode.USER_LINK_DUPLICATE,
                    ErrorCode.LINK_SCRAPING_SERVER_ERROR,
                    ErrorCode.LINK_SCRAPING_TIMEOUT
            }
    )
    @PostMapping
    ApiResponse<UserLinkResponse> createUserLink(
            @Valid @RequestBody UserLinkCreateRequest request,
            @CurrentUserId Long userId
    );


    @Operation(
            summary = "링크 게시물 단건 조회",
            description = "링크 ID로 단건 조회합니다. 비공개 링크는 소유자만 조회 가능합니다."
    )
    @ApiSpec(
            status = HttpStatus.OK,
            errors = {
                    ErrorCode.USER_LINK_NOT_FOUND,
                    ErrorCode.USER_LINK_ACCESS_DENIED
            }
    )
    @GetMapping("/{userLinkId}")
    ApiResponse<UserLinkResponse> getUserLink(
            @Parameter(description = "사용자 링크 ID", required = true, example = "1")
            @PathVariable Long userLinkId,
            @CurrentUserId(required = false) Long userId
    );

    @Operation(
            summary = "링크 게시물 수정",
            description = """
                    링크 정보를 부분 수정합니다. 전달된 필드만 수정되며, 소유자만 수정 가능합니다.

                    **레퍼런스 폴더 변경 방법**
                    1. 특정 폴더로 이동: `referenceId`에 폴더 ID 전달
                    2. 미지정 폴더로 이동: `moveToDefault`를 true로 설정
                    3. 기존 폴더 유지: 둘 다 생략

                    ⚠️ `referenceId`와 `moveToDefault`는 동시에 사용할 수 없습니다.
                    """
    )
    @ApiSpec(
            status = HttpStatus.OK,
            errors = {
                    ErrorCode.VALIDATION_ERROR,
                    ErrorCode.UNAUTHORIZED,
                    ErrorCode.USER_LINK_NOT_FOUND,
                    ErrorCode.USER_LINK_ACCESS_DENIED,
                    ErrorCode.REFERENCE_NOT_FOUND,
                    ErrorCode.REFERENCE_SELECTION_DUPLICATE
            }
    )
    @PatchMapping("/{userLinkId}")
    ApiResponse<UserLinkResponse> updateUserLink(
            @Parameter(description = "사용자 링크 ID", required = true, example = "1")
            @PathVariable Long userLinkId,
            @Valid @RequestBody UserLinkUpdateRequest request,
            @CurrentUserId Long userId
    );

    @Operation(
            summary = "링크 게시물 삭제",
            description = "링크를 삭제합니다. 소유자만 삭제 가능합니다."
    )
    @ApiSpec(
            status = org.springframework.http.HttpStatus.NO_CONTENT,
            errors = {
                    ErrorCode.UNAUTHORIZED,
                    ErrorCode.USER_LINK_NOT_FOUND,
                    ErrorCode.USER_LINK_ACCESS_DENIED
            }
    )
    @DeleteMapping("/{userLinkId}")
    ApiResponse<Void> deleteUserLink(
            @Parameter(description = "사용자 링크 ID", required = true, example = "1")
            @PathVariable Long userLinkId,
            @CurrentUserId Long userId
    );

    // 사용자 링크 목록 조회
    @Operation(
            summary = "링크 게시물 목록 조회(전체/카테고리별)",
            description = """
                    사용자의 링크 목록을 레퍼런스별로 커서 기반 페이징으로 조회합니다.
                    - referenceId가 있으면: 해당 레퍼런스에 속한 링크만 조회
                    - referenceId가 없으면: 전체 링크 조회
                    """
    )
    @ApiSpec(
            status = HttpStatus.OK,
            errors = {
                    ErrorCode.VALIDATION_ERROR,
                    ErrorCode.UNAUTHORIZED,
                    ErrorCode.USER_NOT_FOUND,
                    ErrorCode.REFERENCE_NOT_FOUND,
                    ErrorCode.REFERENCE_ACCESS_DENIED
            }
    )
    @GetMapping
    ApiResponse<PaginationUtils.Cursor.PageResponse<UserLinkListResponse>> getUserLinks(
            @Parameter(description = "레퍼런스 ID (선택, 지정하지 않으면 전체 링크 조회)", example = "1")
            @RequestParam(required = false) Long referenceId,
            @Parameter(description = "커서 (첫 요청 시 null)", example = "10")
            @RequestParam(required = false) String cursor,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
            @CurrentUserId Long userId
    );
}