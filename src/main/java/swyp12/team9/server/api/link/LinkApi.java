package swyp12.team9.server.api.link;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import swyp12.team9.server.api.link.dto.LinkDetailResponse;
import swyp12.team9.server.api.link.dto.LinkResponse;
import swyp12.team9.server.api.link.dto.SaveLinkRequest;
import swyp12.team9.server.api.link.dto.UpdateLinkRequest;
import swyp12.team9.server.domain.userlink.model.LinkStatus;
import swyp12.team9.server.global.annotation.CurrentUserId;

import java.util.List;

@Tag(name = "UserLink", description = "사용자 링크 관리 API")
@RequestMapping("/api/v1/user-links")
public interface LinkApi {

        @Operation(summary = "링크 저장", description = "특정 폴더(Reference)에 링크를 저장합니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "링크 저장 성공", content = @Content(schema = @Schema(implementation = LinkResponse.class))),
                        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                        @ApiResponse(responseCode = "404", description = "사용자 또는 폴더를 찾을 수 없음")
        })
        @PostMapping
        swyp12.team9.server.global.common.dto.ApiResponse<LinkResponse> saveLink(
                        @Valid @RequestBody SaveLinkRequest request,
                        @CurrentUserId Long userId);

        @Operation(summary = "링크 게시물 조회")
        @GetMapping("/{userLinkId}")
        swyp12.team9.server.global.common.dto.ApiResponse<LinkDetailResponse> getLink(
                        @PathVariable Long userLinkId,
                        @CurrentUserId Long userId);

        @Operation(summary = "링크 게시물 목록 조회")
        @GetMapping
        swyp12.team9.server.global.common.dto.ApiResponse<List<LinkDetailResponse>> getLinks(
                        @RequestParam(required = false) String category,
                        @RequestParam(required = false) LinkStatus status,
                        @CurrentUserId Long userId);

        @Operation(summary = "링크 게시물 수정")
        @PatchMapping("/{userLinkId}")
        swyp12.team9.server.global.common.dto.ApiResponse<LinkDetailResponse> updateLink(
                        @PathVariable Long userLinkId,
                        @RequestBody UpdateLinkRequest request,
                        @CurrentUserId Long userId);

        @Operation(summary = "링크 게시물 삭제")
        @DeleteMapping("/{userLinkId}")
        swyp12.team9.server.global.common.dto.ApiResponse<Void> deleteLink(
                        @PathVariable Long userLinkId,
                        @CurrentUserId Long userId);

        @Operation(summary = "링크 게시물 열람 및 조회수 증가 처리")
        @PostMapping("/{userLinkId}/read")
        swyp12.team9.server.global.common.dto.ApiResponse<Void> markAsRead(
                        @PathVariable Long userLinkId,
                        @CurrentUserId Long userId);
}
