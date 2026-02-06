package swyp12.team9.server.api.reference;

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
import swyp12.team9.server.api.reference.dto.ReferenceSortType;
import swyp12.team9.server.api.reference.dto.ReferenceType;
import swyp12.team9.server.api.reference.dto.request.ReferenceCreateRequest;
import swyp12.team9.server.api.reference.dto.request.ReferenceUpdateRequest;
import swyp12.team9.server.api.reference.dto.response.ReferenceListResponse;
import swyp12.team9.server.api.reference.dto.response.ReferenceResponse;
import swyp12.team9.server.global.annotation.ApiSpec;
import swyp12.team9.server.global.annotation.CurrentUserId;
import swyp12.team9.server.global.common.dto.ApiResponse;
import swyp12.team9.server.global.exception.ErrorCode;
import swyp12.team9.server.global.util.PaginationUtils;

/**
 * Reference API 인터페이스 이 인터페이스는 Reference 관련 API 스펙을 정의합니다.
 */
@Tag(name = "Reference View", description = "레퍼런스 폴더 관리 API")
@RequestMapping("/api/v1/references")
public interface ReferenceApi {

    @Operation(
            summary = "레퍼런스 생성",
            description = "새로운 레퍼런스 폴더를 생성합니다."
    )
    @ApiSpec(
            status = HttpStatus.CREATED,
            errors = {
                    ErrorCode.VALIDATION_ERROR,
                    ErrorCode.USER_NOT_FOUND,
                    ErrorCode.REFERENCE_TITLE_DUPLICATE
            }
    )
    @PostMapping
    ApiResponse<ReferenceResponse> createReference(
            @Valid @RequestBody ReferenceCreateRequest request,
            @CurrentUserId Long userId
    );

    @Operation(
            summary = "레퍼런스 단건 조회",
            description = """
                    레퍼런스 ID로 단건 조회합니다. 비공개 레퍼런스는 소유자만 조회 가능합니다.
                    """
    )
    @ApiSpec(
            status = HttpStatus.OK,
            errors = {
                    ErrorCode.REFERENCE_NOT_FOUND,
                    ErrorCode.REFERENCE_ACCESS_DENIED
            }
    )
    @GetMapping("/{referenceId}")
    ApiResponse<ReferenceResponse> getReference(
            @Parameter(description = "레퍼런스 ID", required = true, example = "1")
            @PathVariable Long referenceId,
            @CurrentUserId(required = false) Long userId
    );

    @Operation(
            summary = "레퍼런스 수정",
            description = "레퍼런스 정보를 부분 수정합니다. 전달된 필드만 수정되며, 소유자만 수정 가능합니다."
    )
    @ApiSpec(
            status = HttpStatus.OK,
            errors = {
                    ErrorCode.VALIDATION_ERROR,
                    ErrorCode.REFERENCE_NOT_FOUND,
                    ErrorCode.REFERENCE_ACCESS_DENIED,
                    ErrorCode.REFERENCE_DEFAULT_NOT_MODIFIABLE
            }
    )
    @PatchMapping("/{referenceId}")
    ApiResponse<ReferenceResponse> updateReference(
            @Parameter(description = "레퍼런스 ID", required = true, example = "1")
            @PathVariable Long referenceId,
            @Valid @RequestBody ReferenceUpdateRequest request,
            @CurrentUserId Long userId
    );

    @Operation(
            summary = "레퍼런스 삭제",
            description = "레퍼런스를 삭제합니다. 소유자만 삭제 가능합니다."
    )
    @ApiSpec(
            status = HttpStatus.NO_CONTENT,
            errors = {
                    ErrorCode.REFERENCE_NOT_FOUND,
                    ErrorCode.REFERENCE_ACCESS_DENIED,
                    ErrorCode.REFERENCE_DEFAULT_CANNOT_DELETE
            }
    )
    @DeleteMapping("/{referenceId}")
    ApiResponse<Void> deleteReference(
            @Parameter(description = "레퍼런스 ID", required = true, example = "1")
            @PathVariable Long referenceId,
            @CurrentUserId Long userId
    );

    @Operation(
            summary = "레퍼런스 목록 조회",
            description = """
                    레퍼런스 목록을 커서 기반 페이징으로 조회합니다.
                    미지정 폴더일 경우 isDefault = true 로 표시됩니다.
                    
                    **type 파라미터:**
                    - `all`: 내 전체 레퍼런스 (공개 + 비공개 + 미지정 폴더)
                    - `public`: 내 공개 레퍼런스만
                    - `private`: 내 비공개 레퍼런스만
                    
                    **sortBy 파라미터:**
                    - `latest`: 최신 생성순 (기본값)
                    - `oldest`: 오래된 생성순
                    - `link-count-desc`: 링크 개수 많은 순
                    - `link-count-asc`: 링크 개수 적은 순
                    """
    )
    @ApiSpec(
            status = HttpStatus.OK,
            errors = {
                    ErrorCode.UNAUTHORIZED,
                    ErrorCode.VALIDATION_ERROR
            }
    )
    @GetMapping
    ApiResponse<PaginationUtils.Cursor.PageResponse<ReferenceListResponse>> getReferences(
            @Parameter(description = "조회 타입 (all: 전체, public: 공개, private: 비공개)", example = "all")
            @RequestParam(defaultValue = "ALL") ReferenceType type,
            @Parameter(description = "정렬 타입 (latest: 최신순, oldest: 오래된순, link-count-desc: 링크많은순, link-count-asc: 링크적은순)", example = "latest")
            @RequestParam(defaultValue = "CREATED_DESC") ReferenceSortType sortBy,
            @Parameter(description = "커서 (첫 요청 시 null)", example = "10")
            @RequestParam(required = false) String cursor,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
            @CurrentUserId Long userId
    );

    @Operation(
            summary = "자주 찾는 레퍼런스 조회",
            description = """
                    사용자의 레퍼런스를 링크 개수가 많은 순으로 조회합니다.
                    미지정 폴더(isDefault=true)는 제외됩니다.
                    커서 기반 페이징을 지원하여 무한 스크롤 구현에 적합합니다.
                    
                    **커서 형식:** `referenceId:linkCount` (예: `10:5`)
                    
                    **커서에 linkCount가 포함되는 이유**
                    - 링크 개수(linkCount)로 정렬 시, 동일한 linkCount를 가진 레퍼런스가 여러 개 존재할 수 있습니다.
                    - ID만으로는 정확한 페이징 위치를 특정할 수 없어, linkCount도 함께 사용합니다.
                    - 예: cursor=10:5 → "linkCount < 5" 또는 "linkCount = 5이면서 id < 10"인 레퍼런스 조회
                    
                    **사용 예시:**
                    1. 첫 요청: GET /api/v1/references/frequent?size=20
                    2. 응답의 nextCursor 값(예: "3:18")을 다음 요청에 사용
                    3. 다음 요청: GET /api/v1/references/frequent?size=20&cursor=3:18
                    4. hasNext가 false일 때까지 반복
                    """
    )
    @ApiSpec(
            status = HttpStatus.OK,
            errors = {
                    ErrorCode.UNAUTHORIZED
            }
    )
    @GetMapping("/frequent")
    ApiResponse<PaginationUtils.Cursor.PageResponse<ReferenceListResponse>> getFrequentReferences(
            @Parameter(description = "커서 (첫 요청 시 null)", example = "10:5")
            @RequestParam(required = false) String cursor,
            @Parameter(description = "페이지 크기 (기본값: 20)", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
            @CurrentUserId Long userId
    );

    // ========== 관리자 전용 API ==========

//    @Operation(
//            summary = "[관리자] 모든 비공개 레퍼런스 조회",
//            description = "관리자만 접근 가능. 모든 사용자의 비공개 레퍼런스를 조회합니다."
//    )
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "조회 성공"),
//            @ApiResponse(responseCode = "403", description = "권한 없음 (관리자 아님)")
//    })
//    @PreAuthorize("hasRole('ADMIN')")
//    @GetMapping("/admin/not-public")
//    swyp12.team9.server.global.common.dto.ApiResponse<PaginationUtils.Cursor.PageResponse<ReferenceResponse>> getAllNotPublicReferencesForAdmin(
//            @Parameter(description = "커서 (첫 요청 시 null)", example = "10")
//            @RequestParam(required = false) String cursor,
//            @Parameter(description = "페이지 크기", example = "20")
//            @RequestParam(defaultValue = "20") int size
//    );
}