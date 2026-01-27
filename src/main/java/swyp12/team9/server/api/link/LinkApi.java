package swyp12.team9.server.api.link;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import swyp12.team9.server.api.link.dto.CreateLinkRequest;
import swyp12.team9.server.api.link.dto.LinkResponse;
import swyp12.team9.server.api.link.dto.UpdateLinkRequest;
import swyp12.team9.server.global.annotation.CurrentUserId;
import swyp12.team9.server.global.util.PaginationUtils;

/**
 * Link API 인터페이스
 */
@Tag(name = "Link", description = "링크 관리 API")
@RequestMapping("/api/v1/links")
public interface LinkApi {

    @Operation(summary = "링크 생성", description = "새로운 링크를 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "링크 생성 성공",
                    content = @Content(schema = @Schema(implementation = LinkResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "레퍼런스를 찾을 수 없음")
    })
    @PostMapping
    swyp12.team9.server.global.common.dto.ApiResponse<LinkResponse> createLink(
            @Valid @RequestBody CreateLinkRequest request,
            @CurrentUserId Long userId
    );

    @Operation(summary = "링크 단건 조회", description = "링크 ID로 단건 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "링크를 찾을 수 없음")
    })
    @GetMapping("/{linkId}")
    swyp12.team9.server.global.common.dto.ApiResponse<LinkResponse> getLink(
            @Parameter(description = "링크 ID", required = true, example = "1")
            @PathVariable Long linkId,
            @CurrentUserId(required = false) Long userId
    );

    @Operation(summary = "링크 수정", description = "링크 정보를 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "링크를 찾을 수 없음")
    })
    @PatchMapping("/{linkId}")
    swyp12.team9.server.global.common.dto.ApiResponse<LinkResponse> updateLink(
            @Parameter(description = "링크 ID", required = true, example = "1")
            @PathVariable Long linkId,
            @Valid @RequestBody UpdateLinkRequest request,
            @CurrentUserId Long userId
    );

    @Operation(summary = "링크 삭제", description = "링크를 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "링크를 찾을 수 없음")
    })
    @DeleteMapping("/{linkId}")
    swyp12.team9.server.global.common.dto.ApiResponse<Void> deleteLink(
            @Parameter(description = "링크 ID", required = true, example = "1")
            @PathVariable Long linkId,
            @CurrentUserId Long userId
    );

    @Operation(summary = "링크 열람 처리", description = "링크를 열람 상태로 변경합니다.")
    @PostMapping("/{linkId}/view")
    swyp12.team9.server.global.common.dto.ApiResponse<LinkResponse> markAsViewed(
            @Parameter(description = "링크 ID", required = true, example = "1")
            @PathVariable Long linkId,
            @CurrentUserId Long userId
    );

    @Operation(summary = "즐겨찾기 토글", description = "링크의 즐겨찾기 상태를 토글합니다.")
    @PostMapping("/{linkId}/bookmark")
    swyp12.team9.server.global.common.dto.ApiResponse<LinkResponse> toggleBookmark(
            @Parameter(description = "링크 ID", required = true, example = "1")
            @PathVariable Long linkId,
            @CurrentUserId Long userId
    );

    @Operation(summary = "내 전체 링크 목록 조회", description = "내 모든 링크를 조회합니다.")
    @GetMapping("/my")
    swyp12.team9.server.global.common.dto.ApiResponse<PaginationUtils.Cursor.PageResponse<LinkResponse>> getMyLinks(
            @Parameter(description = "커서", example = "10")
            @RequestParam(required = false) String cursor,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @CurrentUserId Long userId
    );

    @Operation(summary = "레퍼런스(폴더)별 링크 목록 조회", description = "특정 레퍼런스의 링크 목록을 조회합니다.")
    @GetMapping("/reference/{referenceId}")
    swyp12.team9.server.global.common.dto.ApiResponse<PaginationUtils.Cursor.PageResponse<LinkResponse>> getLinksByReference(
            @Parameter(description = "레퍼런스 ID", required = true, example = "1")
            @PathVariable Long referenceId,
            @Parameter(description = "커서", example = "10")
            @RequestParam(required = false) String cursor,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @CurrentUserId(required = false) Long userId
    );

    @Operation(summary = "즐겨찾기 링크 목록 조회", description = "즐겨찾기한 링크 목록을 조회합니다.")
    @GetMapping("/bookmarked")
    swyp12.team9.server.global.common.dto.ApiResponse<PaginationUtils.Cursor.PageResponse<LinkResponse>> getBookmarkedLinks(
            @Parameter(description = "커서", example = "10")
            @RequestParam(required = false) String cursor,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @CurrentUserId Long userId
    );

    // ========== 탐색(Explore) 화면용 API ==========

    @Operation(summary = "[탐색] 공개 링크 목록 조회", description = "공개된 링크 목록을 조회합니다.")
    @GetMapping("/explore")
    swyp12.team9.server.global.common.dto.ApiResponse<PaginationUtils.Cursor.PageResponse<LinkResponse>> getPublicLinks(
            @Parameter(description = "카테고리 ID (없으면 전체 조회)", example = "1")
            @RequestParam(required = false) Long categoryId,
            @Parameter(description = "커서", example = "10")
            @RequestParam(required = false) String cursor,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size
    );
}
