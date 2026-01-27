package swyp12.team9.server.api.link;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import swyp12.team9.server.api.link.dto.CreateLinkRequest;
import swyp12.team9.server.api.link.dto.LinkResponse;
import swyp12.team9.server.api.link.dto.UpdateLinkRequest;
import swyp12.team9.server.domain.link.service.LinkService;
import swyp12.team9.server.global.annotation.CurrentUserId;
import swyp12.team9.server.global.common.dto.ApiResponse;
import swyp12.team9.server.global.util.PaginationUtils;

@Slf4j
@RestController
@RequestMapping("/api/v1/links")
@RequiredArgsConstructor
public class LinkController implements LinkApi {

    private final LinkService linkService;

    @Override
    public ApiResponse<LinkResponse> createLink(
            @Valid @RequestBody CreateLinkRequest request,
            @CurrentUserId Long userId) {

        LinkResponse response = linkService.createLink(userId, request);
        return ApiResponse.created(response, "링크가 생성되었습니다.");
    }

    @Override
    public ApiResponse<LinkResponse> getLink(
            @PathVariable Long linkId,
            @CurrentUserId(required = false) Long userId) {

        LinkResponse response = linkService.getLink(userId, linkId);
        return ApiResponse.ok(response);
    }

    @Override
    public ApiResponse<LinkResponse> updateLink(
            @PathVariable Long linkId,
            @Valid @RequestBody UpdateLinkRequest request,
            @CurrentUserId Long userId) {

        LinkResponse response = linkService.updateLink(userId, linkId, request);
        return ApiResponse.ok(response, "링크가 수정되었습니다.");
    }

    @Override
    public ApiResponse<Void> deleteLink(
            @PathVariable Long linkId,
            @CurrentUserId Long userId) {

        linkService.deleteLink(userId, linkId);
        return ApiResponse.noContent();
    }

    @Override
    public ApiResponse<LinkResponse> markAsViewed(
            @PathVariable Long linkId,
            @CurrentUserId Long userId) {

        LinkResponse response = linkService.markAsViewed(userId, linkId);
        return ApiResponse.ok(response, "링크를 열람 처리했습니다.");
    }

    @Override
    public ApiResponse<LinkResponse> toggleBookmark(
            @PathVariable Long linkId,
            @CurrentUserId Long userId) {

        LinkResponse response = linkService.toggleBookmark(userId, linkId);
        return ApiResponse.ok(response);
    }

    @Override
    public ApiResponse<PaginationUtils.Cursor.PageResponse<LinkResponse>> getMyLinks(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size,
            @CurrentUserId Long userId) {

        PaginationUtils.Cursor.PageResponse<LinkResponse> response =
                linkService.getMyLinks(userId, cursor, size);
        return ApiResponse.ok(response);
    }

    @Override
    public ApiResponse<PaginationUtils.Cursor.PageResponse<LinkResponse>> getLinksByReference(
            @PathVariable Long referenceId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size,
            @CurrentUserId(required = false) Long userId) {

        PaginationUtils.Cursor.PageResponse<LinkResponse> response =
                linkService.getLinksByReference(userId, referenceId, cursor, size);
        return ApiResponse.ok(response);
    }

    @Override
    public ApiResponse<PaginationUtils.Cursor.PageResponse<LinkResponse>> getBookmarkedLinks(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size,
            @CurrentUserId Long userId) {

        PaginationUtils.Cursor.PageResponse<LinkResponse> response =
                linkService.getBookmarkedLinks(userId, cursor, size);
        return ApiResponse.ok(response);
    }

    @Override
    public ApiResponse<PaginationUtils.Cursor.PageResponse<LinkResponse>> getPublicLinks(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size) {

        PaginationUtils.Cursor.PageResponse<LinkResponse> response =
                linkService.getPublicLinksByCategory(categoryId, cursor, size);
        return ApiResponse.ok(response);
    }
}
