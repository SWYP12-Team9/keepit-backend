package swyp12.team9.server.api.reference;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import swyp12.team9.server.api.reference.dto.CreateReferenceRequest;
import swyp12.team9.server.api.reference.dto.ReferenceResponse;
import swyp12.team9.server.api.reference.dto.UpdateReferenceRequest;
import swyp12.team9.server.global.annotation.CurrentUserId;
import swyp12.team9.server.global.util.PaginationUtils;

import java.util.List;

/**
 * Reference API 인터페이스
 * 이 인터페이스는 Reference 관련 API 스펙을 정의합니다.
 * 장점:
 * 1. API 스펙이 명확하게 정의됨 (Swagger 어노테이션)
 * 2. 다른 서비스에서 이 인터페이스만 의존하면 API 스펙을 알 수 있음
 * 3. Controller는 이 인터페이스를 구현하여 Swagger를 의존할 필요가 없음
 * 4. API 문서 자동 생성 (Swagger UI)
 */
@Tag(name = "Reference View", description = "레퍼런스 폴더 관리 API")
@RequestMapping("/api/v1/references")
public interface ReferenceApi {

        @Operation(summary = "레퍼런스 생성", description = "새로운 레퍼런스 폴더를 생성합니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "레퍼런스 생성 성공", content = @Content(schema = @Schema(implementation = ReferenceResponse.class))),
                        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
        })
        @PostMapping
        swyp12.team9.server.global.common.dto.ApiResponse<ReferenceResponse> createReference(
                        @Valid @RequestBody CreateReferenceRequest request,
                        @CurrentUserId Long userId);

        //
        @Operation(summary = "레퍼런스 단건 조회", description = "레퍼런스 ID로 단건 조회합니다. 비공개 레퍼런스는 소유자만 조회 가능합니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "레퍼런스 조회 성공", content = @Content(schema = @Schema(implementation = ReferenceResponse.class))),
                        @ApiResponse(responseCode = "403", description = "권한 없음 (비공개 레퍼런스)"),
                        @ApiResponse(responseCode = "404", description = "레퍼런스를 찾을 수 없음")
        })
        @GetMapping("/{referenceId}")
        swyp12.team9.server.global.common.dto.ApiResponse<ReferenceResponse> getReference(
                        @Parameter(description = "레퍼런스 ID", required = true, example = "1") @PathVariable Long referenceId,
                        @CurrentUserId(required = false) Long userId);

        //
        @Operation(summary = "레퍼런스 수정", description = "레퍼런스 정보를 수정합니다. 소유자만 수정 가능합니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "레퍼런스 수정 성공", content = @Content(schema = @Schema(implementation = ReferenceResponse.class))),
                        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
                        @ApiResponse(responseCode = "403", description = "권한 없음 (소유자가 아님)"),
                        @ApiResponse(responseCode = "404", description = "레퍼런스를 찾을 수 없음")
        })
        @PatchMapping("/{referenceId}")
        swyp12.team9.server.global.common.dto.ApiResponse<ReferenceResponse> updateReference(
                        @Parameter(description = "레퍼런스 ID", required = true, example = "1") @PathVariable Long referenceId,
                        @Valid @RequestBody UpdateReferenceRequest request,
                        @CurrentUserId Long userId);

        //
        @Operation(summary = "레퍼런스 삭제", description = "레퍼런스를 삭제합니다. 소유자만 삭제 가능합니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "204", description = "레퍼런스 삭제 성공"),
                        @ApiResponse(responseCode = "403", description = "권한 없음 (소유자가 아님)"),
                        @ApiResponse(responseCode = "404", description = "레퍼런스를 찾을 수 없음")
        })
        @DeleteMapping("/{referenceId}")
        swyp12.team9.server.global.common.dto.ApiResponse<Void> deleteReference(
                        @Parameter(description = "레퍼런스 ID", required = true, example = "1") @PathVariable Long referenceId,
                        @CurrentUserId Long userId);

        // ========== 커서 기반 페이지네이션 API ==========

        @Operation(summary = "나의 레퍼런스 목록 조회 (커서 페이징)", description = "현재 사용자의 레퍼런스를 커서 기반 페이징으로 조회합니다. 무한 스크롤에 적합합니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "목록 조회 성공"),
                        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
        })
        @GetMapping("/my")
        swyp12.team9.server.global.common.dto.ApiResponse<PaginationUtils.Cursor.PageResponse<ReferenceResponse>> getMyReferences(
                        @Parameter(description = "커서 (첫 요청 시 null)", example = "10") @RequestParam(required = false) String cursor,
                        @Parameter(description = "페이지 크기", example = "20") @RequestParam(defaultValue = "20") int size,
                        @CurrentUserId Long userId);

        @Operation(summary = "공개 레퍼런스 목록 조회 (커서 페이징)", description = "공개된 레퍼런스를 커서 기반 페이징으로 조회합니다.")
        @GetMapping("/public")
        swyp12.team9.server.global.common.dto.ApiResponse<PaginationUtils.Cursor.PageResponse<ReferenceResponse>> getPublicReferences(
                        @Parameter(description = "커서 (첫 요청 시 null)", example = "10") @RequestParam(required = false) String cursor,
                        @Parameter(description = "페이지 크기", example = "20") @RequestParam(defaultValue = "20") int size);

        @Operation(summary = "비공개 레퍼런스 목록 조회 (커서 페이징)", description = "현재 사용자의 비공개 레퍼런스를 커서 기반 페이징으로 조회합니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "목록 조회 성공"),
                        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
        })
        @GetMapping("/not-public")
        swyp12.team9.server.global.common.dto.ApiResponse<PaginationUtils.Cursor.PageResponse<ReferenceResponse>> getNotPublicReferences(
                        @Parameter(description = "커서 (첫 요청 시 null)", example = "10") @RequestParam(required = false) String cursor,
                        @Parameter(description = "페이지 크기", example = "20") @RequestParam(defaultValue = "20") int size,
                        @CurrentUserId Long userId);

        // 관리자
        // ========== 관리자 전용 API ==========

        @Operation(summary = "[관리자] 모든 비공개 레퍼런스 조회", description = "관리자만 접근 가능. 모든 사용자의 비공개 레퍼런스를 조회합니다.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "조회 성공"),
                        @ApiResponse(responseCode = "403", description = "권한 없음 (관리자 아님)")
        })
        @PreAuthorize("hasRole('ADMIN')")
        @GetMapping("/admin/not-public")
        swyp12.team9.server.global.common.dto.ApiResponse<PaginationUtils.Cursor.PageResponse<ReferenceResponse>> getAllNotPublicReferencesForAdmin(
                        @Parameter(description = "커서 (첫 요청 시 null)", example = "10") @RequestParam(required = false) String cursor,
                        @Parameter(description = "페이지 크기", example = "20") @RequestParam(defaultValue = "20") int size);

}