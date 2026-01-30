package swyp12.team9.server.api.reference;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import swyp12.team9.server.api.reference.dto.CreateReferenceRequest;
import swyp12.team9.server.api.reference.dto.ReferenceResponse;
import swyp12.team9.server.api.reference.dto.ReferenceType;
import swyp12.team9.server.api.reference.dto.UpdateReferenceRequest;
import swyp12.team9.server.domain.reference.service.ReferenceService;
import swyp12.team9.server.global.annotation.CurrentUserId;
import swyp12.team9.server.global.common.dto.ApiResponse;
import swyp12.team9.server.global.util.PaginationUtils;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/references")
@RequiredArgsConstructor
public class ReferenceController implements ReferenceApi {

    private final ReferenceService referenceService;

    // 레퍼런스 생성
    @Override
    public ApiResponse<ReferenceResponse> createReference(
            @Valid @RequestBody CreateReferenceRequest request,
            @CurrentUserId Long userId) {

        ReferenceResponse response = referenceService.createReference(userId, request);
        return ApiResponse.created(response, "레퍼런스 폴더가 생성되었습니다.");
    }

    // 레퍼런스 단건 조회
    @Override
    public ApiResponse<ReferenceResponse> getReference(
            @PathVariable Long referenceId,
            @CurrentUserId(required = false) Long userId) {

        ReferenceResponse response = referenceService.getReference(userId, referenceId);
        return ApiResponse.ok(response);
    }

    // 레퍼런스 수정
    @Override
    public ApiResponse<ReferenceResponse> updateReference(
            @PathVariable Long referenceId,
            @Valid @RequestBody UpdateReferenceRequest request,
            @CurrentUserId Long userId) {

        ReferenceResponse response = referenceService.updateReference(userId, referenceId, request);
        return ApiResponse.ok(response, "레퍼런스가 수정되었습니다.");
    }

    // 레퍼런스 삭제
    @Override
    public ApiResponse<Void> deleteReference(
            @PathVariable Long referenceId,
            @CurrentUserId Long userId) {

        referenceService.deleteReference(userId, referenceId);
        return ApiResponse.noContent();
    }

    // ========== 레퍼런스 목록 조회 (통합 API) ==========
    @Override
    public ApiResponse<PaginationUtils.Cursor.PageResponse<ReferenceResponse>> getReferences(
            @RequestParam(defaultValue = "ALL") ReferenceType type,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size,
            @CurrentUserId(required = false) Long userId) {

        PaginationUtils.Cursor.PageResponse<ReferenceResponse> response =
                referenceService.getReferences(userId, type, cursor, size);
        return ApiResponse.ok(response);
    }

    // ========== 관리자 전용 API ==========
//    @Override
//    @PreAuthorize("hasRole('ADMIN')")
//    public ApiResponse<PaginationUtils.Cursor.PageResponse<ReferenceResponse>> getAllNotPublicReferencesForAdmin(
//            @RequestParam(required = false) String cursor,
//            @RequestParam(defaultValue = "20") int size) {
//
//        log.info("관리자 비공개 레퍼런스 조회 요청 - cursor: {}, size: {}", cursor, size);
//
//        PaginationUtils.Cursor.PageResponse<ReferenceResponse> response =
//                referenceService.getAllNotPublicReferences(cursor, size);
//
//        return ApiResponse.ok(response, "관리자 비공개 레퍼런스 조회 완료");
//    }
}