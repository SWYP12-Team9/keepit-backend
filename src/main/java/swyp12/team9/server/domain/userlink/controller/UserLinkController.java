package swyp12.team9.server.domain.userlink.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import swyp12.team9.server.domain.userlink.dto.request.UserLinkCreateRequest;
import swyp12.team9.server.domain.userlink.dto.request.UserLinkUpdateRequest;
import swyp12.team9.server.domain.userlink.dto.response.UserLinkListResponse;
import swyp12.team9.server.domain.userlink.dto.response.UserLinkResponse;
import swyp12.team9.server.domain.userlink.service.UserLinkService;
import swyp12.team9.server.global.annotation.CurrentUserId;
import swyp12.team9.server.global.common.dto.ApiResponse;
import swyp12.team9.server.global.util.PaginationUtils;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/user-links")
@RequiredArgsConstructor
public class UserLinkController implements UserLinkApi {

    private final UserLinkService userLinkService;

    // 사용자 링크 생성
    @Override
    public ApiResponse<UserLinkResponse> createUserLink(
            @Valid @RequestBody UserLinkCreateRequest request,
            @CurrentUserId Long userId) {

        UserLinkResponse response = userLinkService.createUserLink(userId, request);
        return ApiResponse.created(response, "링크가 저장되었습니다.");
    }

    // 사용자 링크 단건 조회
    @Override
    public ApiResponse<UserLinkResponse> getUserLink(
            @PathVariable Long userLinkId,
            @CurrentUserId(required = false) Long userId) {

        UserLinkResponse response = userLinkService.getUserLink(userId, userLinkId);
        return ApiResponse.ok(response, "링크를 조회했습니다.");
    }

    @Override
    public ApiResponse<UserLinkListResponse> getUserLinkPreview(
            @PathVariable Long userLinkId,
            @CurrentUserId Long userId) {

        UserLinkListResponse response = userLinkService.getUserLinkPreview(userId, userLinkId);
        return ApiResponse.ok(response);
    }

    // 사용자 링크 수정
    @Override
    public ApiResponse<UserLinkResponse> updateUserLink(
            @PathVariable Long userLinkId,
            @Valid @RequestBody UserLinkUpdateRequest request,
            @CurrentUserId Long userId) {

        UserLinkResponse response = userLinkService.updateUserLink(userId, userLinkId, request);
        return ApiResponse.ok(response, "링크가 수정되었습니다.");
    }

    // 사용자 링크 삭제
    @Override
    public ApiResponse<Void> deleteUserLink(
            @PathVariable Long userLinkId,
            @CurrentUserId Long userId) {

        userLinkService.deleteUserLink(userId, userLinkId);
        return ApiResponse.noContent();
    }

    // 사용자 링크 목록 조회 (레퍼런스별)
    @Override
    public ApiResponse<PaginationUtils.Cursor.PageResponse<UserLinkListResponse>> getUserLinks(
            @RequestParam(required = false) Long referenceId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size,
            @CurrentUserId Long userId) {

        PaginationUtils.Cursor.PageResponse<UserLinkListResponse> response =
                userLinkService.getUserLinksByReferenceId(userId, referenceId, cursor, size);
        return ApiResponse.ok(response);
    }
}
