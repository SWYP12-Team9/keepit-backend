package swyp12.team9.server.domain.reference.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swyp12.team9.server.api.reference.dto.CreateReferenceRequest;
import swyp12.team9.server.api.reference.dto.ReferenceResponse;
import swyp12.team9.server.api.reference.dto.ReferenceType;
import swyp12.team9.server.api.reference.dto.UpdateReferenceRequest;
import swyp12.team9.server.domain.reference.exception.ReferenceAccessDeniedException;
import swyp12.team9.server.domain.reference.exception.ReferenceNotFoundException;
import swyp12.team9.server.domain.reference.model.Reference;
import swyp12.team9.server.domain.reference.repository.ReferenceRepository;
import swyp12.team9.server.domain.user.exception.UserNotFoundException;
import swyp12.team9.server.domain.user.model.User;
import swyp12.team9.server.domain.user.repository.UserRepository;
import swyp12.team9.server.global.util.PaginationUtils;
import swyp12.team9.server.global.util.PaginationUtils.Cursor.PageResponse;

import swyp12.team9.server.domain.reference.model.enums.ReferenceCategory;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReferenceService {

    private final ReferenceRepository referenceRepository;
    private final UserRepository userRepository;

    // 레퍼런스 생성
    @Transactional
    public ReferenceResponse createReference(Long userId, CreateReferenceRequest request) {

        User user = getUserById(userId);
        Reference reference = Reference.create(user, request.title(), request.description(), request.isPublic());
        Reference savedReference = referenceRepository.save(reference);

        log.info("레퍼런스 생성 완료 - userId: {}, referenceId: {}", userId, savedReference.getId());
        return ReferenceResponse.from(savedReference);
    }

    /**
     * 사용자의 기본 폴더 (A, B, C, D, E) 초기화
     */
    @Transactional
    public void initializeDefaultFolders(User user) {
        for (ReferenceCategory category : ReferenceCategory.values()) {
            if (!referenceRepository.existsByUserIdAndTitle(user.getId(), category.getTitle())) {
                Reference reference = Reference.builder()
                        .user(user)
                        .title(category.getTitle())
                        .category(category)
                        .description(category.getTitle() + " 관련 보관함")
                        .isPublic(true)
                        .build();
                referenceRepository.save(reference);
            }
        }
        log.info("사용자 기본 폴더 초기화 완료 - userId: {}", user.getId());
    }

    // 레퍼런스 단건 조회
    /*
     * - 공개 레퍼런스: 누구나 조회 가능
     * - 비공개 레퍼런스: 소유자만 조회 가능
     */
    public ReferenceResponse getReference(Long userId, Long referenceId) {
        Reference reference = getReferenceById(referenceId);

        // 공개 레퍼런스면 누구나 조회 가능
        if (reference.getIsPublic()) {
            return ReferenceResponse.from(reference);
        }

        // 비공개 레퍼런스는 소유자만 조회 가능
        if (userId == null) {
            throw new ReferenceAccessDeniedException("비공개 레퍼런스는 로그인이 필요합니다.");
        }

        reference.validateOwner(userId);
        return ReferenceResponse.from(reference);
    }

    /**
     * 레퍼런스 목록 조회 (통합 API - 커서 페이징)
     *
     * @param userId 현재 사용자 ID (null 가능)
     * @param type   조회 타입 (ALL: 전체, PUBLIC: 공개, PRIVATE: 비공개)
     * @param cursor 페이지 커서
     * @param size   페이지 크기
     */
    public PageResponse<ReferenceResponse> getReferences(
            Long userId, ReferenceType type, String cursor, int size) {

        return switch (type) {
            case ALL -> getUserReferencesCursor(userId, cursor, size);
            case PUBLIC -> getPublicReferencesCursor(cursor, size);
            case PRIVATE -> getNotPublicReferencesCursor(userId, cursor, size);
        };
    }

    /**
     * 사용자의 레퍼런스 목록 조회 (커서 페이징) - 전체
     */
    private PageResponse<ReferenceResponse> getUserReferencesCursor(
            Long userId, String cursor, int size) {

        if (userId == null) {
            throw new ReferenceAccessDeniedException("전체 레퍼런스 조회는 로그인이 필요합니다.");
        }

        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId);
        }

        PageRequest pageRequest = PageRequest.of(0, size + 1);

        List<Reference> references;
        if (cursor == null) {
            references = referenceRepository.findByUserIdOrderByIdDesc(userId, pageRequest);
        } else {
            Long cursorId = Long.parseLong(cursor);
            references = referenceRepository.findByUserIdAndIdLessThanOrderByIdDesc(userId, cursorId, pageRequest);
        }

        return buildCursorResponse(references, size);
    }

    /**
     * 공개 레퍼런스 목록 조회 (커서 페이징)
     */
    private PageResponse<ReferenceResponse> getPublicReferencesCursor(String cursor, int size) {

        PageRequest pageRequest = PageRequest.of(0, size + 1);

        List<Reference> references;
        if (cursor == null) {
            references = referenceRepository.findByIsPublicTrueOrderByIdDesc(pageRequest);
        } else {
            Long cursorId = Long.parseLong(cursor);
            references = referenceRepository.findByIsPublicTrueAndIdLessThanOrderByIdDesc(cursorId, pageRequest);
        }

        return buildCursorResponse(references, size);
    }

    /**
     * 비공개 레퍼런스 목록 조회 (커서 페이징)
     */
    private PageResponse<ReferenceResponse> getNotPublicReferencesCursor(
            Long userId, String cursor, int size) {

        if (userId == null) {
            throw new ReferenceAccessDeniedException("비공개 레퍼런스 조회는 로그인이 필요합니다.");
        }

        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId);
        }

        PageRequest pageRequest = PageRequest.of(0, size + 1);

        List<Reference> references;
        if (cursor == null) {
            references = referenceRepository.findByUserIdAndIsPublicFalseOrderByIdDesc(
                    userId, pageRequest);
        } else {
            Long cursorId = Long.parseLong(cursor);
            references = referenceRepository.findByUserIdAndIsPublicFalseAndIdLessThanOrderByIdDesc(
                    userId, cursorId, pageRequest);
        }

        return buildCursorResponse(references, size);
    }

    // 레퍼런스 수정
    @Transactional
    public ReferenceResponse updateReference(Long userId, Long referenceId, UpdateReferenceRequest request) {
        Reference reference = getReferenceById(referenceId);

        // 소유자 검증
        reference.validateOwner(userId);

        reference.update(request.title(), request.description(), request.isPublic());

        log.info("레퍼런스 수정 완료 - userId: {}, referenceId: {}", userId, referenceId);
        return ReferenceResponse.from(reference);
    }

    // 레퍼런스 삭제
    @Transactional
    public void deleteReference(Long userId, Long referenceId) {
        Reference reference = getReferenceById(referenceId);

        // 소유자 검증
        reference.validateOwner(userId);

        referenceRepository.delete(reference);

        log.info("레퍼런스 삭제 완료 - userId: {}, referenceId: {}", userId, referenceId);
    }

    /**
     * 커서 기반 응답 생성 헬퍼 메서드
     */
    private PageResponse<ReferenceResponse> buildCursorResponse(
            List<Reference> references, int size) {

        // 결과가 없으면 빈 응답
        if (references.isEmpty()) {
            return PageResponse.empty();
        }

        // hasNext 판단: size + 1개를 조회했으므로, size보다 크면 다음 페이지 있음
        boolean hasNext = references.size() > size;

        // 실제 반환할 데이터는 size개만
        List<Reference> content = hasNext ? references.subList(0, size) : references;

        // 다음 커서는 마지막 아이템의 ID
        String nextCursor = hasNext ? String.valueOf(content.get(content.size() - 1).getId()) : null;

        // DTO 변환
        List<ReferenceResponse> responses = content.stream()
                .map(ReferenceResponse::from)
                .collect(Collectors.toList());

        return PageResponse.of(responses, nextCursor, hasNext);
    }

    // 사용자 조회 메서드
    private User getUserById(Long userId) {

        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId);
        }

        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));
    }

    // 레퍼런스 조회 메서드
    private Reference getReferenceById(Long referenceId) {
        return referenceRepository.findById(referenceId)
                .orElseThrow(() -> new ReferenceNotFoundException("레퍼런스를 찾을 수 없습니다. ID: " + referenceId));
    }

    // ========== 관리자 전용 메서드 ==========

    /**
     * [관리자] 모든 비공개 레퍼런스 목록 조회 (커서 페이징)
     *
     * @param cursor 페이지 커서 (null이면 첫 페이지)
     * @param size   페이지 크기
     * @return 모든 사용자의 비공개 레퍼런스 목록
     */
    public PaginationUtils.Cursor.PageResponse<ReferenceResponse> getAllNotPublicReferences(
            String cursor, int size) {

        PageRequest pageRequest = PageRequest.of(0, size + 1);

        List<Reference> references;
        if (cursor == null) {
            // 첫 페이지: 모든 비공개 레퍼런스 조회
            references = referenceRepository.findByIsPublicFalseOrderByIdDesc(pageRequest);
            log.debug("관리자 비공개 레퍼런스 첫 페이지 조회 - size: {}", size);
        } else {
            // 다음 페이지: cursor 이후의 비공개 레퍼런스 조회
            Long cursorId = Long.parseLong(cursor);
            references = referenceRepository.findByIsPublicFalseAndIdLessThanOrderByIdDesc(cursorId, pageRequest);
            log.debug("관리자 비공개 레퍼런스 다음 페이지 조회 - cursor: {}, size: {}", cursor, size);
        }

        PaginationUtils.Cursor.PageResponse<ReferenceResponse> response = buildCursorResponse(references, size);

        log.info("관리자 비공개 레퍼런스 조회 완료 - contents: {}, hasNext: {}",
                response.getContents().size(), response.isHasNext());

        return response;
    }
}
