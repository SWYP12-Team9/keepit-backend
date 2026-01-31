package swyp12.team9.server.api.userlink;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import swyp12.team9.server.api.userlink.dto.request.UserLinkCreateRequest;
import swyp12.team9.server.api.userlink.dto.request.UserLinkUpdateRequest;
import swyp12.team9.server.api.userlink.dto.response.UserLinkListResponse;
import swyp12.team9.server.api.userlink.dto.response.UserLinkResponse;
import swyp12.team9.server.global.annotation.CurrentUserId;
import swyp12.team9.server.global.util.PaginationUtils;

/**
 * UserLink API 인터페이스
 * 사용자 링크 관련 API 스펙을 정의합니다.
 */
@Tag(name = "UserLink", description = "사용자 링크 관리 API")
@RequestMapping("/api/v1/user-links")
public interface UserLinkApi {

    @Operation(
            summary = "링크 게시물 생성",
            description = "새로운 링크를 저장합니다. URL로 기존 Link가 있으면 재사용하고, 없으면 새로 생성합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "링크 저장 성공",
                    content = @Content(schema = @Schema(implementation = UserLinkResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효하지 않은 URL 등)"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @PostMapping
    swyp12.team9.server.global.common.dto.ApiResponse<UserLinkResponse> createUserLink(
            @Valid @RequestBody UserLinkCreateRequest request,
            @CurrentUserId Long userId
    );

    @Operation(
            summary = "링크 게시물 단건 조회",
            description = "링크 ID로 단건 조회합니다. 비공개 링크는 소유자만 조회 가능합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = UserLinkResponse.class))
            ),
            @ApiResponse(responseCode = "403", description = "권한 없음 (비공개 링크)"),
            @ApiResponse(responseCode = "404", description = "링크를 찾을 수 없음")
    })
    @GetMapping("/{userLinkId}")
    swyp12.team9.server.global.common.dto.ApiResponse<UserLinkResponse> getUserLink(
            @Parameter(description = "사용자 링크 ID", required = true, example = "1")
            @PathVariable Long userLinkId,
            @CurrentUserId(required = false) Long userId
    );

    @Operation(
            summary = "링크 게시물 수정",
            description = "링크 정보를 부분 수정합니다. 전달된 필드만 수정되며, 소유자만 수정 가능합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "수정 성공",
                    content = @Content(schema = @Schema(implementation = UserLinkResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (소유자가 아님)"),
            @ApiResponse(responseCode = "404", description = "링크를 찾을 수 없음")
    })
    @PatchMapping("/{userLinkId}")
    swyp12.team9.server.global.common.dto.ApiResponse<UserLinkResponse> updateUserLink(
            @Parameter(description = "사용자 링크 ID", required = true, example = "1")
            @PathVariable Long userLinkId,
            @Valid @RequestBody UserLinkUpdateRequest request,
            @CurrentUserId Long userId
    );

    @Operation(
            summary = "링크 게시물 삭제",
            description = "링크를 삭제합니다. 소유자만 삭제 가능합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (소유자가 아님)"),
            @ApiResponse(responseCode = "404", description = "링크를 찾을 수 없음")
    })
    @DeleteMapping("/{userLinkId}")
    swyp12.team9.server.global.common.dto.ApiResponse<Void> deleteUserLink(
            @Parameter(description = "사용자 링크 ID", required = true, example = "1")
            @PathVariable Long userLinkId,
            @CurrentUserId Long userId
    );

    @Operation(
            summary = "링크 게시물 열람(읽음) 처리",
            description = "링크 게시물을 열람 상태로 변경하고 조회수를 증가시킵니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "열람 처리 성공",
                    content = @Content(schema = @Schema(implementation = UserLinkResponse.class))
            ),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "링크를 찾을 수 없음")
    })
    @PostMapping("/{userLinkId}/read")
    swyp12.team9.server.global.common.dto.ApiResponse<UserLinkResponse> markAsRead(
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
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "목록 조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (소유자 아님)"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping
    swyp12.team9.server.global.common.dto.ApiResponse<PaginationUtils.Cursor.PageResponse<UserLinkListResponse>> getUserLinks(
            @Parameter(description = "레퍼런스 ID (선택, 지정하지 않으면 전체 링크 조회)", example = "1")
            @RequestParam(required = false) Long referenceId,
            @Parameter(description = "커서 (첫 요청 시 null)", example = "10")
            @RequestParam(required = false) String cursor,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @CurrentUserId Long userId
    );
}