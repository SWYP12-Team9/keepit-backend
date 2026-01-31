package swyp12.team9.server.domain.reference.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swyp12.team9.server.api.reference.dto.ReferenceSortType;
import swyp12.team9.server.api.reference.dto.ReferenceType;
import swyp12.team9.server.api.reference.dto.request.ReferenceCreateRequest;
import swyp12.team9.server.api.reference.dto.request.ReferenceUpdateRequest;
import swyp12.team9.server.api.reference.dto.response.ReferenceListResponse;
import swyp12.team9.server.api.reference.dto.response.ReferenceResponse;
import swyp12.team9.server.domain.auth.exception.UnauthorizedException;
import swyp12.team9.server.domain.reference.exception.ReferenceTitleDuplicateException;
import swyp12.team9.server.domain.reference.exception.ReferenceNotFoundException;
import swyp12.team9.server.domain.reference.model.Reference;
import swyp12.team9.server.domain.reference.repository.ReferenceRepository;
import swyp12.team9.server.domain.user.exception.UserNotFoundException;
import swyp12.team9.server.domain.user.model.User;
import swyp12.team9.server.domain.user.repository.UserRepository;
import swyp12.team9.server.domain.userlink.repository.UserLinkRepository;
import swyp12.team9.server.global.util.PaginationUtils.Cursor.PageResponse;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReferenceService {

    private static final Long UNSPECIFIED_REFERENCE_ID = -1L;

    private final ReferenceRepository referenceRepository;
    private final UserRepository userRepository;
    private final UserLinkRepository userLinkRepository;

    /**
     * 레퍼런스 생성
     */
    @Transactional
    public ReferenceResponse createReference(Long userId, ReferenceCreateRequest request) {

        User user = getUserById(userId);

        validateDuplicateTitle(userId, request.title());

        Reference reference = Reference.create(
                user,
                request.title(),
                request.description(),
                request.isPublic(),
                request.colorCode()
        );

        Reference savedReference = referenceRepository.save(reference);

        log.info("레퍼런스 생성 완료 - userId: {}, referenceId: {}", userId, savedReference.getId());
        return ReferenceResponse.from(savedReference);
    }

    private void validateDuplicateTitle(Long userId, String title) {
        if (referenceRepository.existsByUserIdAndTitle(userId, title)) {
            throw new ReferenceTitleDuplicateException();
        }
    }

    /**
     * 레퍼런스 단건 조회
     * - 공개 레퍼런스: 누구나 조회 가능
     * - 비공개 레퍼런스: 소유자만 조회 가능
     */
    public ReferenceResponse getReference(Long userId, Long referenceId) {

        Reference reference = getReferenceById(referenceId);

        // 공개 레퍼런스 -> 모두 조회 가능 & 비공개 레퍼런스 -> 소유자만 조회 가능
        validateReferenceAccess(reference, userId);

        return ReferenceResponse.from(reference);
    }

    /**
     * 레퍼런스 목록 조회 (커서 페이징)
     *
     * @param userId 현재 사용자 ID (null 가능)
     * @param type   조회 타입 (ALL: 전체, PUBLIC: 공개, PRIVATE: 비공개)
     * @param sortBy 정렬 타입 (CREATED_DESC: 최신 생성순, LINK_COUNT_DESC: 링크 많은 순)
     * @param cursor 페이지 커서
     * @param size   페이지 크기
     */
    public PageResponse<ReferenceListResponse> getReferences(Long userId, ReferenceType type, ReferenceSortType sortBy, String cursor, int size) {

        Long cursorId = (cursor == null) ? null : Long.parseLong(cursor);

        // 폴더 목록 조회
        List<ReferenceListResponse> results = referenceRepository.findAllWithLinkCount(userId, type, sortBy, cursorId, size);

        return buildPageResponse(results, size);
    }

    private PageResponse<ReferenceListResponse> buildPageResponse(List<ReferenceListResponse> results, int size) {

        if (results.isEmpty()) {
            return PageResponse.empty();
        }

        boolean hasNext = results.size() > size;
        List<ReferenceListResponse> content = hasNext ? results.subList(0, size) : results;

        String nextCursor = String.valueOf(content.get(content.size() - 1).id());

        return PageResponse.of(content, nextCursor, hasNext);
    }

    // 레퍼런스 수정
    @Transactional
    public ReferenceResponse updateReference(Long userId, Long referenceId, ReferenceUpdateRequest request) {
        Reference reference = getReferenceById(referenceId);

        // 소유자 검증
        reference.validateOwner(userId);

        reference.update(
                request.title(),
                request.description(),
                request.isPublic(),
                request.colorCode());

        log.info("레퍼런스 수정 완료 - userId: {}, referenceId: {}", userId, referenceId);
        return ReferenceResponse.from(reference);
    }

    /**
     * 레퍼런스 삭제
     * - 기본 미지정 폴더는 삭제 불가
     */
    @Transactional
    public void deleteReference(Long userId, Long referenceId) {
        Reference reference = getReferenceById(referenceId);

        // 소유자 검증
        reference.validateOwner(userId);

        // 기본 폴더 삭제 검증
        reference.validateDeletable();

        referenceRepository.delete(reference);

        log.info("레퍼런스 삭제 완료 - userId: {}, referenceId: {}", userId, referenceId);
    }

    /**
     * 사용자의 기본 미지정 폴더 조회 또는 생성
     */
    @Transactional
    public Reference getOrCreateDefaultReference(Long userId) {
        User user = getUserById(userId);

        return referenceRepository.findByUserIdAndIsDefaultTrue(userId)
                .orElseGet(() -> {
                    Reference defaultReference = Reference.createDefault(user);
                    Reference saved = referenceRepository.save(defaultReference);
                    log.info("기본 미지정 폴더 생성 완료 - userId: {}, referenceId: {}", userId, saved.getId());
                    return saved;
                });
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

    private void validateReferenceAccess(Reference reference, Long userId) {
        // 공개 레퍼런스면 모두 조회 가능
        if (Boolean.TRUE.equals(reference.getIsPublic())) {
            return;
        }

        // 비공개 레퍼런스면 본인만 조회 가능
        if (userId == null) {
            throw new UnauthorizedException();
        }

        reference.validateOwner(userId);
    }

    // ========== 관리자 전용 메서드 ==========
//
//    /**
//     * [관리자] 모든 비공개 레퍼런스 목록 조회 (커서 페이징)
//     *
//     * @param cursor 페이지 커서 (null이면 첫 페이지)
//     * @param size   페이지 크기
//     * @return 모든 사용자의 비공개 레퍼런스 목록
//     */
//    public PaginationUtils.Cursor.PageResponse<ReferenceResponse> getAllNotPublicReferences(
//            String cursor, int size) {
//
//        PageRequest pageRequest = PageRequest.of(0, size + 1);
//
//        List<Reference> references;
//        if (cursor == null) {
//            // 첫 페이지: 모든 비공개 레퍼런스 조회
//            references = referenceRepository.findByIsPublicFalseOrderByIdDesc(pageRequest);
//            log.debug("관리자 비공개 레퍼런스 첫 페이지 조회 - size: {}", size);
//        } else {
//            // 다음 페이지: cursor 이후의 비공개 레퍼런스 조회
//            Long cursorId = Long.parseLong(cursor);
//            references = referenceRepository.findByIsPublicFalseAndIdLessThanOrderByIdDesc(cursorId, pageRequest);
//            log.debug("관리자 비공개 레퍼런스 다음 페이지 조회 - cursor: {}, size: {}", cursor, size);
//        }
//
//        PaginationUtils.Cursor.PageResponse<ReferenceResponse> response = buildCursorResponse(references, size);
//
//        log.info("관리자 비공개 레퍼런스 조회 완료 - contents: {}, hasNext: {}",
//                response.getContents().size(), response.isHasNext());
//
//        return response;
//    }
}
