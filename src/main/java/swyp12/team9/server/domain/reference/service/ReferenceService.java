package swyp12.team9.server.domain.reference.service;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swyp12.team9.server.api.reference.dto.CreateReferenceRequest;
import swyp12.team9.server.api.reference.dto.ReferenceResponse;
import swyp12.team9.server.api.reference.dto.UpdateReferenceRequest;
import swyp12.team9.server.api.user.dto.response.UserResponse;
import swyp12.team9.server.domain.reference.exception.ReferenceNotFoundException;
import swyp12.team9.server.domain.reference.model.Reference;
import swyp12.team9.server.domain.reference.repository.ReferenceRepository;
import swyp12.team9.server.domain.user.exception.UserNotFoundException;
import swyp12.team9.server.domain.user.model.User;
import swyp12.team9.server.domain.user.repository.UserRepository;
import swyp12.team9.server.global.util.PaginationUtils;
import swyp12.team9.server.global.util.PaginationUtils.Cursor.PageResponse;

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

    // 레퍼런스 단건 조회
    public ReferenceResponse getReference(Long userId, Long referenceId) {
        Reference reference = getReferenceById(referenceId);

        // 비공개 레퍼런스는 소유자만 조회 가능
        if (!reference.getIsPublic()) {
            reference.validateOwner(userId);
        }

        return ReferenceResponse.from(reference);
    }

    /**
     * 사용자의 레퍼런스 목록 조회 (커서 페이징)
     */
    public PageResponse<ReferenceResponse> getUserReferencesCursor(
            Long userId, String cursor, int size) {

        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId);
        }

        // size + 1개를 조회해서 hasNext 판단
        PageRequest pageRequest = PageRequest.of(0, size + 1);

        List<Reference> references;
        if (cursor == null) {
            // 첫 페이지
            references = referenceRepository.findByUserIdOrderByIdDesc(userId, pageRequest);
        } else {
            // 다음 페이지
            Long cursorId = Long.parseLong(cursor);
            references = referenceRepository.findByUserIdAndIdLessThanOrderByIdDesc(userId, cursorId, pageRequest);
        }

        return buildCursorResponse(references, size);
    }

    /**
     * 공개 레퍼런스 목록 조회 (커서 페이징)
     */
    public PaginationUtils.Cursor.PageResponse<ReferenceResponse> getPublicReferencesCursor(
            String cursor, int size) {

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
    public PaginationUtils.Cursor.PageResponse<ReferenceResponse> getNotPublicReferencesCursor(
            Long userId, String cursor, int size) {

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
    public PaginationUtils.Cursor.PageResponse<ReferenceResponse> getAllNotPublicReferencesCursor(
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
