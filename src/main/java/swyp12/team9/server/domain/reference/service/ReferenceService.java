package swyp12.team9.server.domain.reference.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swyp12.team9.server.api.reference.dto.ReferenceCursor;
import swyp12.team9.server.api.reference.dto.ReferenceSortType;
import swyp12.team9.server.api.reference.dto.ReferenceType;
import swyp12.team9.server.api.reference.dto.request.ReferenceCreateRequest;
import swyp12.team9.server.api.reference.dto.request.ReferenceUpdateRequest;
import swyp12.team9.server.api.reference.dto.response.ReferenceListResponse;
import swyp12.team9.server.api.reference.dto.response.ReferenceResponse;
import swyp12.team9.server.domain.auth.exception.UnauthorizedException;
import swyp12.team9.server.domain.reference.exception.ReferenceNotFoundException;
import swyp12.team9.server.domain.reference.exception.ReferenceTitleDuplicateException;
import swyp12.team9.server.domain.reference.model.Reference;
import swyp12.team9.server.domain.reference.repository.ReferenceRepository;
import swyp12.team9.server.domain.user.exception.UserNotFoundException;
import swyp12.team9.server.domain.user.model.User;
import swyp12.team9.server.domain.user.repository.UserRepository;
import swyp12.team9.server.domain.userlink.repository.UserLinkRepository;
import swyp12.team9.server.global.util.PaginationUtils.Cursor.PageResponse;

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
     * 레퍼런스 단건 조회 - 공개 레퍼런스: 누구나 조회 가능 - 비공개 레퍼런스: 소유자만 조회 가능
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
     * @param sortBy 정렬 타입 (CREATED_DESC: 최신순, CREATED_ASC: 오래된순, LINK_COUNT_DESC: 링크많은순, LINK_COUNT_ASC: 링크적은순)
     * @param cursor 페이지 커서
     * @param size   페이지 크기
     */
    public PageResponse<ReferenceListResponse> getReferences(Long userId,
                                                             ReferenceType type,
                                                             ReferenceSortType sortBy,
                                                             String cursor,
                                                             int size
    ) {
        ReferenceCursor referenceCursor = ReferenceCursor.from(cursor, sortBy);

        List<ReferenceListResponse> results = referenceRepository.findAllWithLinkCount(
                userId, type, sortBy, referenceCursor, size
        );

        return buildPageResponse(results, size, sortBy);
    }

    private PageResponse<ReferenceListResponse> buildPageResponse(
            List<ReferenceListResponse> results,
            int size,
            ReferenceSortType sortBy
    ) {
        if (results.isEmpty()) {
            return PageResponse.empty();
        }

        boolean hasNext = results.size() > size;
        List<ReferenceListResponse> content = hasNext ? results.subList(0, size) : results;

        ReferenceListResponse last = content.get(content.size() - 1);
        String nextCursor = buildNextCursor(last, sortBy);

        return PageResponse.of(content, nextCursor, hasNext);
    }

    private String buildNextCursor(ReferenceListResponse last, ReferenceSortType sortBy) {
        if (sortBy == ReferenceSortType.LINK_COUNT_DESC || sortBy == ReferenceSortType.LINK_COUNT_ASC) {
            return last.id() + ":" + last.linkCount();
        }
        return String.valueOf(last.id());
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
     * 레퍼런스 삭제 - 기본 미지정 폴더는 삭제 불가
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

    /**
     * 레퍼런스 생성 (엔티티 반환) - 다른 서비스에서 레퍼런스 생성 후 엔티티가 필요할 때 사용 - 예: UserLink 생성 시 새 레퍼런스 폴더를 함께 생성하는 경우
     *
     * @param userId    사용자 ID
     * @param title     레퍼런스 제목
     * @param colorCode 색상 코드 (null 가능)
     * @return 생성된 Reference 엔티티
     */
    @Transactional
    public Reference createReferenceEntity(Long userId, String title, String colorCode) {
        User user = getUserById(userId);

        validateDuplicateTitle(userId, title);

        Reference reference = Reference.create(
                user,
                title,
                null,  // description은 null로 설정
                true, // 기본 : 공개
                colorCode
        );

        Reference savedReference = referenceRepository.save(reference);

        log.info("레퍼런스 생성 완료 (엔티티 반환) - userId: {}, referenceId: {}, title: {}",
                userId, savedReference.getId(), title);

        return savedReference;
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
                .orElseThrow(ReferenceNotFoundException::new);
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
