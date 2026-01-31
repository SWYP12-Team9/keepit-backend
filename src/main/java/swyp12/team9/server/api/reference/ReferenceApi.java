package swyp12.team9.server.api.reference;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;
import swyp12.team9.server.api.reference.dto.ReferenceSortType;
import swyp12.team9.server.api.reference.dto.ReferenceType;
import swyp12.team9.server.api.reference.dto.request.ReferenceCreateRequest;
import swyp12.team9.server.api.reference.dto.request.ReferenceUpdateRequest;
import swyp12.team9.server.api.reference.dto.response.ReferenceListResponse;
import swyp12.team9.server.api.reference.dto.response.ReferenceResponse;
import swyp12.team9.server.global.annotation.CurrentUserId;
import swyp12.team9.server.global.util.PaginationUtils;

/**
 * Reference API 인터페이스
 * 이 인터페이스는 Reference 관련 API 스펙을 정의합니다.
 */
@Tag(name = "Reference View", description = "레퍼런스 폴더 관리 API")
@RequestMapping("/api/v1/references")
public interface ReferenceApi {

    @Operation(
            summary = "레퍼런스 생성",
            description = "새로운 레퍼런스 폴더를 생성합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "레퍼런스 생성 성공",
                    content = @Content(schema = @Schema(implementation = ReferenceResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "사용자의 레퍼런스 제목 중복")
    })
    @PostMapping
    swyp12.team9.server.global.common.dto.ApiResponse<ReferenceResponse> createReference(
            @Valid @RequestBody ReferenceCreateRequest request,
            @CurrentUserId Long userId
    );

    @Operation(
            summary = "레퍼런스 단건 조회",
            description = """
                    레퍼런스 ID로 단건 조회합니다. 비공개 레퍼런스는 소유자만 조회 가능합니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "레퍼런스 조회 성공",
                    content = @Content(schema = @Schema(implementation = ReferenceResponse.class))
            ),
            @ApiResponse(responseCode = "403", description = "권한 없음 (비공개 레퍼런스)"),
            @ApiResponse(responseCode = "404", description = "레퍼런스를 찾을 수 없음")
    })
    @GetMapping("/{referenceId}")
    swyp12.team9.server.global.common.dto.ApiResponse<ReferenceResponse> getReference(
            @Parameter(description = "레퍼런스 ID", required = true, example = "1")
            @PathVariable Long referenceId,
            @CurrentUserId(required = false) Long userId
    );

    @Operation(
            summary = "레퍼런스 수정",
            description = "레퍼런스 정보를 부분 수정합니다. 전달된 필드만 수정되며, 소유자만 수정 가능합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "레퍼런스 수정 성공",
                    content = @Content(schema = @Schema(implementation = ReferenceResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (소유자가 아님)"),
            @ApiResponse(responseCode = "404", description = "레퍼런스를 찾을 수 없음")
    })
    @PatchMapping("/{referenceId}")
    swyp12.team9.server.global.common.dto.ApiResponse<ReferenceResponse> updateReference(
            @Parameter(description = "레퍼런스 ID", required = true, example = "1")
            @PathVariable Long referenceId,
            @Valid @RequestBody ReferenceUpdateRequest request,
            @CurrentUserId Long userId
    );

    @Operation(
            summary = "레퍼런스 삭제",
            description = "레퍼런스를 삭제합니다. 소유자만 삭제 가능합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "레퍼런스 삭제 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (소유자가 아님)"),
            @ApiResponse(responseCode = "404", description = "레퍼런스를 찾을 수 없음")
    })
    @DeleteMapping("/{referenceId}")
    swyp12.team9.server.global.common.dto.ApiResponse<Void> deleteReference(
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
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "목록 조회 성공",
            content = @Content(schema = @Schema(implementation = ReferenceListResponse.class))
            )
    })
    @GetMapping
    swyp12.team9.server.global.common.dto.ApiResponse<PaginationUtils.Cursor.PageResponse<ReferenceListResponse>> getReferences(
            @Parameter(description = "조회 타입 (all: 전체, public: 공개, private: 비공개)", example = "all")
            @RequestParam(defaultValue = "ALL") ReferenceType type,
            @Parameter(description = "정렬 타입 (latest: 최신순, oldest: 오래된순, link-count-desc: 링크많은순, link-count-asc: 링크적은순)", example = "latest")
            @RequestParam(defaultValue = "CREATED_DESC") ReferenceSortType sortBy,
            @Parameter(description = "커서 (첫 요청 시 null)", example = "10")
            @RequestParam(required = false) String cursor,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size,
            @CurrentUserId(required = false) Long userId
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