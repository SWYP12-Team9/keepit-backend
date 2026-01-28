package swyp12.team9.server.domain.userlink.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swyp12.team9.server.api.userlink.dto.CreateUserLinkRequest;
import swyp12.team9.server.api.userlink.dto.UpdateUserLinkRequest;
import swyp12.team9.server.api.userlink.dto.UserLinkResponse;
import swyp12.team9.server.api.userlink.dto.UserLinkType;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.link.repository.LinkRepository;
import swyp12.team9.server.domain.user.exception.UserNotFoundException;
import swyp12.team9.server.domain.user.model.User;
import swyp12.team9.server.domain.user.repository.UserRepository;
import swyp12.team9.server.domain.userlink.exception.UserLinkAccessDeniedException;
import swyp12.team9.server.domain.userlink.exception.UserLinkDuplicateException;
import swyp12.team9.server.domain.userlink.exception.UserLinkNotFoundException;
import swyp12.team9.server.domain.userlink.model.UserLink;
import swyp12.team9.server.domain.userlink.repository.UserLinkRepository;
import swyp12.team9.server.global.util.PaginationUtils.Cursor.PageResponse;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserLinkService {

    private final UserLinkRepository userLinkRepository;
    private final LinkRepository linkRepository;
    private final UserRepository userRepository;

    /**
     * 사용자 링크 생성
     * - URL로 기존 Link가 있으면 재사용, 없으면 새로 생성
     */
    @Transactional
    public UserLinkResponse createUserLink(Long userId, CreateUserLinkRequest request) {
        User user = getUserById(userId);

        // URL로 기존 Link 조회 또는 새로 생성
        Link link = linkRepository.findByUrl(request.url())
                .orElseGet(() -> {
                    Link newLink = Link.builder()
                            .url(request.url())
                            .build();
                    return linkRepository.save(newLink);
                });

        // 이미 같은 링크를 저장했는지 확인
        if (userLinkRepository.existsByUserIdAndLinkId(userId, link.getId())) {
            throw new UserLinkDuplicateException();
        }

        // UserLink 생성
        UserLink userLink = UserLink.builder()
                .user(user)
                .link(link)
                .why(request.why())
                .isPublic(request.isPublic())
                .memo(request.memo())
                .build();

        UserLink savedUserLink = userLinkRepository.save(userLink);

        log.info("사용자 링크 생성 완료 - userId: {}, userLinkId: {}, url: {}",
                userId, savedUserLink.getId(), request.url());

        return UserLinkResponse.from(savedUserLink);
    }

    /**
     * 사용자 링크 단건 조회
     * - 공개 링크: 누구나 조회 가능
     * - 비공개 링크: 소유자만 조회 가능
     * - 소유자가 조회 시 조회수 증가
     */
    @Transactional
    public UserLinkResponse getUserLink(Long userId, Long userLinkId) {
        UserLink userLink = getUserLinkById(userLinkId);

        // 소유자가 조회하는 경우 조회수 증가
        boolean isOwner = userId != null && userLink.getUser().getId().equals(userId);
        if (isOwner) {
            userLink.incrementViewCount();
            log.debug("조회수 증가 - userLinkId: {}, viewCount: {}", userLinkId, userLink.getViewCount());
        }

        // 공개 링크면 누구나 조회 가능
        if (userLink.getIsPublic()) {
            return UserLinkResponse.from(userLink);
        }

        // 비공개 링크는 소유자만 조회 가능
        if (userId == null) {
            throw new UserLinkAccessDeniedException("비공개 링크는 로그인이 필요합니다.");
        }

        userLink.validateOwner(userId);
        return UserLinkResponse.from(userLink);
    }

    /**
     * 사용자 링크 수정
     */
    @Transactional
    public UserLinkResponse updateUserLink(Long userId, Long userLinkId, UpdateUserLinkRequest request) {
        UserLink userLink = getUserLinkById(userLinkId);

        // 소유자 검증
        userLink.validateOwner(userId);

        // 수정 (null이면 기존값 유지)
        userLink.updateUserLink(
                request.why() != null ? request.why() : userLink.getWhy(),
                request.isPublic() != null ? request.isPublic() : userLink.getIsPublic(),
                request.memo() != null ? request.memo() : userLink.getMemo()
        );

        log.info("사용자 링크 수정 완료 - userId: {}, userLinkId: {}", userId, userLinkId);
        return UserLinkResponse.from(userLink);
    }

    /**
     * 사용자 링크 삭제
     */
    @Transactional
    public void deleteUserLink(Long userId, Long userLinkId) {
        UserLink userLink = getUserLinkById(userLinkId);

        // 소유자 검증
        userLink.validateOwner(userId);

        userLinkRepository.delete(userLink);

        log.info("사용자 링크 삭제 완료 - userId: {}, userLinkId: {}", userId, userLinkId);
    }

    /**
     * 사용자 링크 읽음 처리
     */
    @Transactional
    public UserLinkResponse markAsRead(Long userId, Long userLinkId) {
        UserLink userLink = getUserLinkById(userLinkId);

        // 소유자 검증
        userLink.validateOwner(userId);

        userLink.markAsRead();

        log.info("사용자 링크 읽음 처리 완료 - userId: {}, userLinkId: {}", userId, userLinkId);
        return UserLinkResponse.from(userLink);
    }

    /**
     * 사용자 링크 목록 조회 (통합 API - 커서 페이징)
     *
     * @param userId 현재 사용자 ID (null 가능)
     * @param type   조회 타입 (ALL: 전체, PUBLIC: 공개, PRIVATE: 비공개)
     * @param cursor 페이지 커서
     * @param size   페이지 크기
     */
    public PageResponse<UserLinkResponse> getUserLinks(
            Long userId, UserLinkType type, String cursor, int size) {

        return switch (type) {
            case ALL -> getMyUserLinksCursor(userId, cursor, size);
            case PUBLIC -> getPublicUserLinksCursor(cursor, size);
            case PRIVATE -> getNotPublicUserLinksCursor(userId, cursor, size);
        };
    }

    /**
     * 나의 UserLink 목록 조회 (커서 페이징) - 전체
     */
    private PageResponse<UserLinkResponse> getMyUserLinksCursor(Long userId, String cursor, int size) {
        if (userId == null) {
            throw new UserLinkAccessDeniedException("전체 링크 조회는 로그인이 필요합니다.");
        }

        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId);
        }

        PageRequest pageRequest = PageRequest.of(0, size + 1);

        List<UserLink> userLinks;
        if (cursor == null) {
            userLinks = userLinkRepository.findByUserIdOrderByIdDesc(userId, pageRequest);
        } else {
            Long cursorId = Long.parseLong(cursor);
            userLinks = userLinkRepository.findByUserIdAndIdLessThanOrderByIdDesc(userId, cursorId, pageRequest);
        }

        return buildCursorResponse(userLinks, size);
    }

    /**
     * 공개 UserLink 목록 조회 (커서 페이징)
     */
    private PageResponse<UserLinkResponse> getPublicUserLinksCursor(String cursor, int size) {
        PageRequest pageRequest = PageRequest.of(0, size + 1);

        List<UserLink> userLinks;
        if (cursor == null) {
            userLinks = userLinkRepository.findByIsPublicTrueOrderByIdDesc(pageRequest);
        } else {
            Long cursorId = Long.parseLong(cursor);
            userLinks = userLinkRepository.findByIsPublicTrueAndIdLessThanOrderByIdDesc(cursorId, pageRequest);
        }

        return buildCursorResponse(userLinks, size);
    }

    /**
     * 나의 비공개 UserLink 목록 조회 (커서 페이징)
     */
    private PageResponse<UserLinkResponse> getNotPublicUserLinksCursor(Long userId, String cursor, int size) {
        if (userId == null) {
            throw new UserLinkAccessDeniedException("비공개 링크 조회는 로그인이 필요합니다.");
        }

        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId);
        }

        PageRequest pageRequest = PageRequest.of(0, size + 1);

        List<UserLink> userLinks;
        if (cursor == null) {
            userLinks = userLinkRepository.findByUserIdAndIsPublicFalseOrderByIdDesc(userId, pageRequest);
        } else {
            Long cursorId = Long.parseLong(cursor);
            userLinks = userLinkRepository.findByUserIdAndIsPublicFalseAndIdLessThanOrderByIdDesc(
                    userId, cursorId, pageRequest);
        }

        return buildCursorResponse(userLinks, size);
    }

    // ========== Helper 메서드 ==========

    /**
     * 커서 기반 응답 생성 헬퍼 메서드
     */
    private PageResponse<UserLinkResponse> buildCursorResponse(List<UserLink> userLinks, int size) {
        if (userLinks.isEmpty()) {
            return PageResponse.empty();
        }

        boolean hasNext = userLinks.size() > size;
        List<UserLink> content = hasNext ? userLinks.subList(0, size) : userLinks;
        String nextCursor = hasNext ? String.valueOf(content.get(content.size() - 1).getId()) : null;

        List<UserLinkResponse> responses = content.stream()
                .map(UserLinkResponse::from)
                .collect(Collectors.toList());

        return PageResponse.of(responses, nextCursor, hasNext);
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));
    }

    private UserLink getUserLinkById(Long userLinkId) {
        return userLinkRepository.findById(userLinkId)
                .orElseThrow(() -> new UserLinkNotFoundException("사용자 링크를 찾을 수 없습니다. ID: " + userLinkId));
    }
}
