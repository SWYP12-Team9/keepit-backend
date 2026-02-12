package swyp12.team9.server.domain.userlink.service;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swyp12.team9.server.api.userlink.dto.request.UserLinkCreateRequest;
import swyp12.team9.server.api.userlink.dto.request.UserLinkUpdateRequest;
import swyp12.team9.server.api.userlink.dto.response.UserLinkListResponse;
import swyp12.team9.server.api.userlink.dto.response.UserLinkResponse;
import swyp12.team9.server.domain.link.exception.LinkNotFoundException;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.link.repository.LinkRepository;
import swyp12.team9.server.domain.link.service.LinkService;
import swyp12.team9.server.domain.reference.exception.ReferenceNotFoundException;
import swyp12.team9.server.domain.reference.model.Reference;
import swyp12.team9.server.domain.reference.repository.ReferenceRepository;
import swyp12.team9.server.domain.reference.service.ReferenceService;
import swyp12.team9.server.domain.referenceuserlink.model.ReferenceUserLink;
import swyp12.team9.server.domain.referenceuserlink.repository.ReferenceUserLinkRepository;
import swyp12.team9.server.domain.user.exception.UserNotFoundException;
import swyp12.team9.server.domain.user.model.User;
import swyp12.team9.server.domain.user.repository.UserRepository;
import swyp12.team9.server.domain.userlink.exception.ReferenceSelectionDuplicateException;
import swyp12.team9.server.domain.userlink.exception.UserLinkAccessDeniedException;
import swyp12.team9.server.domain.userlink.exception.UserLinkDuplicateException;
import swyp12.team9.server.domain.userlink.exception.UserLinkNotFoundException;
import swyp12.team9.server.domain.userlink.model.UserLink;
import swyp12.team9.server.domain.userlink.repository.UserLinkRepository;
import swyp12.team9.server.global.util.PaginationUtils.Cursor.PageResponse;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserLinkService {

    private final UserLinkRepository userLinkRepository;
    private final LinkRepository linkRepository;
    private final LinkService linkService;
    private final UserRepository userRepository;
    private final ReferenceRepository referenceRepository;
    private final ReferenceUserLinkRepository referenceUserLinkRepository;
    private final ReferenceService referenceService;

    /**
     * 사용자 링크 생성 - 중복 체크를 먼저 수행한 후 Link 생성 - LinkService를 통해 Link 스크래핑 로직 처리 referenceId와 newReference는 둘 중 하나만 선택 둘 다
     * null이면 기본 미지정 폴더에 자동 분류
     */
    @Transactional
    public UserLinkResponse createUserLink(Long userId, UserLinkCreateRequest request) {
        User user = getUserById(userId);

        // referenceId와 newReference 동시 사용 검증
        boolean hasReferenceId = request.referenceId() != null;
        boolean hasNewReference = request.newReference() != null && request.newReference().title() != null;

        if (hasReferenceId && hasNewReference) {
            throw new ReferenceSelectionDuplicateException();
        }

        // URL로 기존 Link 조회
        Link link = linkRepository.findFirstByUrl(request.url()).orElse(null);

        // Link가 이미 존재하는 경우, 중복 체크 먼저 수행
        if (link != null && userLinkRepository.existsByUserIdAndLinkId(userId, link.getId())) {
            throw new UserLinkDuplicateException();
        }

        // 링크가 없으면 생성
        if (link == null) {
            try {
                link = linkRepository.save(linkService.createLink(request.url()));
            } catch (DataIntegrityViolationException e) {
                // 동시 요청으로 다른 쓰레드가 먼저 같은 URL의 Link를 생성한 경우
                link = linkRepository.findFirstByUrl(request.url())
                        .orElseThrow(() -> new LinkNotFoundException());
                log.info("동시 요청 감지 - 기존 Link 재사용: linkId={}", link.getId());
            }
        }

        // UserLink 생성
        UserLink userLink = UserLink.builder()
                .user(user)
                .link(link)
                .why(request.why())
                .memo(request.memo())
                .build();

        UserLink savedUserLink = userLinkRepository.save(userLink);

        // ReferenceUserLink 생성 (기존 폴더 OR 새 폴더 OR 미지정)
        Reference reference;

        if (hasReferenceId) {
            // 1. 기존 레퍼런스 폴더 선택한 경우
            reference = getReferenceById(request.referenceId());
            reference.validateOwner(userId);
        } else if (hasNewReference) {
            // 2. 새 레퍼런스 폴더 생성 옵션 선택한 경우
            reference = referenceService.createReferenceEntity(
                    userId,
                    request.newReference().title(),
                    request.newReference().colorCode()
            );
            log.info("새 레퍼런스 폴더 생성 - userId: {}, referenceId: {}, title: {}",
                    userId, reference.getId(), request.newReference().title());
        } else {
            // 3. 아무것도 선택하지 않은 경우, 기본 미지정 폴더에 자동 분류
            reference = referenceService.getOrCreateDefaultReference(userId);
        }

        // ReferenceUserLink 엔티티 생성 및 저장
        ReferenceUserLink referenceUserLink = ReferenceUserLink.builder()
                .reference(reference)
                .userLink(savedUserLink)
                .build();
        referenceUserLinkRepository.save(referenceUserLink);

        log.info("사용자 링크 생성 완료 - userId: {}, userLinkId: {}, referenceId: {}, url: {}",
                userId, savedUserLink.getId(), reference.getId(), request.url());

        return UserLinkResponse.from(savedUserLink, reference);
    }

    /**
     * 사용자 링크 단건 조회 - 소유자만 조회 가능 (N:N 관계로 공개/비공개 복잡도 증가) - 소유자가 조회 시 조회수 증가
     */
    @Transactional
    public UserLinkResponse getUserLink(Long userId, Long userLinkId) {
        UserLink userLink = getUserLinkById(userLinkId);

        // 소유자 검증
        if (userId == null) {
            throw new UserLinkAccessDeniedException();
        }
        userLink.validateOwner(userId);

        // 읽음 처리(조회수 증가)
        userLink.markAsRead();
        log.debug("조회수 증가 - userLinkId: {}, viewCount: {}", userLinkId, userLink.getViewCount());

        // UserLink에 연결된 Reference 조회 (단일)
        Reference reference = referenceUserLinkRepository.findByUserLinkId(userLinkId).stream()
                .map(ReferenceUserLink::getReference)
                .findFirst()
                .orElse(null);

        return UserLinkResponse.from(userLink, reference);
    }

    /**
     * 사용자 링크 수정
     * - referenceId가 제공되면 해당 폴더로 이동
     * - moveToDefault가 true면 미지정 폴더로 이동
     * - 둘 다 없으면 기존 폴더 유지
     */
    @Transactional
    public UserLinkResponse updateUserLink(Long userId, Long userLinkId, UserLinkUpdateRequest request) {
        UserLink userLink = getUserLinkById(userLinkId);

        // 소유자 검증
        userLink.validateOwner(userId);

        // referenceId와 moveToDefault 동시 사용 검증
        if (request.referenceId() != null && Boolean.TRUE.equals(request.moveToDefault())) {
            throw new ReferenceSelectionDuplicateException();
        }

        // 수정 (null이면 기존값 유지)
        userLink.updateUserLink(
                request.why() != null ? request.why() : userLink.getWhy(),
                request.memo() != null ? request.memo() : userLink.getMemo()
        );

        // Reference 처리
        Reference reference;
        if (Boolean.TRUE.equals(request.moveToDefault())) {
            // 1. 미지정 폴더로 이동
            referenceUserLinkRepository.deleteByUserLinkId(userLinkId);

            reference = referenceService.getOrCreateDefaultReference(userId);

            ReferenceUserLink referenceUserLink = ReferenceUserLink.builder()
                    .reference(reference)
                    .userLink(userLink)
                    .build();
            referenceUserLinkRepository.save(referenceUserLink);

            log.info("사용자 링크 미지정 폴더로 이동 - userId: {}, userLinkId: {}", userId, userLinkId);
        } else if (request.referenceId() != null) {
            // 2. 지정된 폴더로 이동
            referenceUserLinkRepository.deleteByUserLinkId(userLinkId);

            reference = getReferenceById(request.referenceId());
            reference.validateOwner(userId);

            ReferenceUserLink referenceUserLink = ReferenceUserLink.builder()
                    .reference(reference)
                    .userLink(userLink)
                    .build();
            referenceUserLinkRepository.save(referenceUserLink);
        } else {
            // 3. 기존 폴더 유지
            reference = referenceUserLinkRepository.findByUserLinkId(userLinkId).stream()
                    .map(ReferenceUserLink::getReference)
                    .findFirst()
                    .orElse(null);
        }

        log.info("사용자 링크 수정 완료 - userId: {}, userLinkId: {}, referenceId: {}",
                userId, userLinkId, reference != null ? reference.getId() : "null");

        return UserLinkResponse.from(userLink, reference);
    }

    /**
     * 사용자 링크 삭제 - ReferenceUserLink도 함께 삭제
     */
    @Transactional
    public void deleteUserLink(Long userId, Long userLinkId) {
        UserLink userLink = getUserLinkById(userLinkId);

        // 소유자 검증
        userLink.validateOwner(userId);

        // ReferenceUserLink 먼저 삭제
        referenceUserLinkRepository.deleteByUserLinkId(userLinkId);

        // UserLink 삭제
        userLinkRepository.delete(userLink);

        log.info("사용자 링크 삭제 완료 - userId: {}, userLinkId: {}", userId, userLinkId);
    }


    /**
     * 사용자 링크 목록 조회 (커서 페이징)
     *
     * @param userId      현재 사용자 ID
     * @param referenceId 레퍼런스 ID (null이면 전체 조회, 값이 있으면 특정 레퍼런스 조회)
     * @param cursor      커서 (null이면 첫 페이지)
     * @param size        페이지 크기
     * @return 커서 기반 페이징 응답
     */
    public PageResponse<UserLinkListResponse> getUserLinksByReferenceId(
            Long userId, Long referenceId, String cursor, int size) {

        validateUser(userId);

        // referenceId가 있으면 소유자 검증
        Reference targetReference = null;
        if (referenceId != null) {
            targetReference = getReferenceById(referenceId);
            targetReference.validateOwner(userId);
        }

        PageRequest pageRequest = PageRequest.of(0, size + 1);
        Long cursorId = cursor != null ? Long.parseLong(cursor) : null;

        // referenceId로 조회 (null이면 전체, 값이 있으면 특정 레퍼런스)
        List<UserLink> userLinks = userLinkRepository.findUserLinksWithCursor(
                userId, referenceId, cursorId, pageRequest);

        return buildUserLinkListResponse(userLinks, size, targetReference);
    }

    private void validateUser(Long userId) {
        if (userId == null) {
            throw new UserLinkAccessDeniedException();
        }

        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException();
        }
    }

    /**
     * UserLink 목록을 UserLinkListResponse로 변환
     *
     * @param targetReference 조회한 레퍼런스 (null이면 각 UserLink에 연결된 Reference 조회)
     */
    private PageResponse<UserLinkListResponse> buildUserLinkListResponse(
            List<UserLink> userLinks, int size, Reference targetReference) {

        if (userLinks.isEmpty()) {
            return PageResponse.empty();
        }

        boolean hasNext = userLinks.size() > size;
        List<UserLink> content = hasNext ? userLinks.subList(0, size) : userLinks;
        String nextCursor = hasNext ? String.valueOf(content.get(content.size() - 1).getId()) : null;

        List<UserLinkListResponse> responses = content.stream()
                .map(userLink -> {
                    Reference reference;
                    if (targetReference != null) {
                        // referenceId로 조회한 경우, 해당 Reference 사용
                        reference = targetReference;
                    } else {
                        // 전체 조회인 경우, UserLink에 연결된 첫 번째 Reference 조회
                        reference = referenceUserLinkRepository.findByUserLinkId(userLink.getId()).stream()
                                .map(ReferenceUserLink::getReference)
                                .findFirst()
                                .orElse(null);
                    }

                    return UserLinkListResponse.of(userLink, reference);
                })
                .collect(Collectors.toList());

        return PageResponse.of(responses, nextCursor, hasNext);
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
    }

    private UserLink getUserLinkById(Long userLinkId) {
        return userLinkRepository.findById(userLinkId).orElseThrow(UserLinkNotFoundException::new);
    }

    private Reference getReferenceById(Long referenceId) {
        return referenceRepository.findById(referenceId).orElseThrow(ReferenceNotFoundException::new);
    }
}
