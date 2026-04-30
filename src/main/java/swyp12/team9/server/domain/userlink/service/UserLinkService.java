package swyp12.team9.server.domain.userlink.service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swyp12.team9.server.domain.userlink.dto.request.UserLinkCreateRequest;
import swyp12.team9.server.domain.userlink.dto.request.UserLinkUpdateRequest;
import swyp12.team9.server.domain.userlink.dto.response.UserLinkListResponse;
import swyp12.team9.server.domain.userlink.dto.response.UserLinkResponse;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.link.service.LinkService;
import swyp12.team9.server.domain.reference.exception.ReferenceNotFoundException;
import swyp12.team9.server.domain.reference.model.Reference;
import swyp12.team9.server.domain.reference.repository.ReferenceRepository;
import swyp12.team9.server.domain.reference.service.ReferenceService;
import swyp12.team9.server.domain.reference.relation.model.ReferenceUserLink;
import swyp12.team9.server.domain.reference.relation.repository.ReferenceUserLinkRepository;
import swyp12.team9.server.domain.user.exception.UserNotFoundException;
import swyp12.team9.server.domain.user.model.User;
import swyp12.team9.server.domain.user.repository.UserRepository;
import swyp12.team9.server.domain.userlink.event.UserLinkCreatedEvent;
import swyp12.team9.server.domain.userlink.event.UserLinkDeletedEvent;
import swyp12.team9.server.domain.userlink.event.UserLinkChatbotReindexEvent;
import swyp12.team9.server.domain.userlink.exception.ReferenceSelectionDuplicateException;
import swyp12.team9.server.domain.userlink.exception.UserLinkAccessDeniedException;
import swyp12.team9.server.domain.userlink.exception.UserLinkDuplicateException;
import swyp12.team9.server.domain.userlink.exception.UserLinkNotFoundException;
import swyp12.team9.server.domain.userlink.model.UserLink;
import swyp12.team9.server.domain.userlink.repository.UserLinkRepository;
import swyp12.team9.server.global.util.PaginationUtils.Cursor.PageResponse;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserLinkService {

    private static final ConcurrentHashMap<String, Object> URL_LOCKS = new ConcurrentHashMap<>();

    private final UserLinkRepository userLinkRepository;
    private final LinkService linkService;
    private final UserRepository userRepository;
    private final ReferenceRepository referenceRepository;
    private final ReferenceUserLinkRepository referenceUserLinkRepository;
    private final ReferenceService referenceService;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transactionTemplate;

    /**
     * 사용자 링크 생성 - 중복 체크를 먼저 수행한 후 Link 생성 - LinkService를 통해 Link 스크래핑 로직 처리 referenceId와 newReference는 둘 중 하나만 선택 둘 다
     * null이면 기본 미지정 폴더에 자동 분류
     */
    public UserLinkResponse createUserLink(Long userId, UserLinkCreateRequest request) {
        User user = getUserById(userId); // 커넥션 1 사용

        // referenceId와 newReference 동시 사용 검증
        boolean hasReferenceId = request.referenceId() != null;
        boolean hasNewReference = request.newReference() != null && request.newReference().title() != null;

        if (hasReferenceId && hasNewReference) {
            throw new ReferenceSelectionDuplicateException();
        }

        // URL 단위 락으로 동시 요청 시 중복 Link 생성 방지
        String urlHash = Link.generateUrlHash(request.url());
        Object lock = URL_LOCKS.computeIfAbsent(urlHash, k -> new Object());
        Link link;

        try {
            synchronized (lock) {
                // 이 단계에서는 트랜잭션을 점유하지 않아 커넥션 낭비와 데드락 발생을 방지
                // Link 저장 시(getOrSavePlaceholderLink)에만 잠깐 독립 커넥션을 사용
                link = linkService.getOrCreateLink(request.url(), userId);
            }
        }
        finally {
            URL_LOCKS.remove(urlHash);
        }

        return transactionTemplate.execute(status -> {
            // 같은 사용자의 기존 저장 건이 있으면, 처리 중/실패 상태에서는 기존 항목을 그대로 반환한다.
            UserLink existingUserLink = userLinkRepository.findByUserIdAndLinkId(userId, link.getId()).orElse(null);
            if (existingUserLink != null) {
                if (!link.isReady()) {
                    Reference existingReference = referenceUserLinkRepository.findByUserLinkId(existingUserLink.getId()).stream()
                            .map(ReferenceUserLink::getReference)
                            .findFirst()
                            .orElse(null);
                    return UserLinkResponse.from(existingUserLink, existingReference);
                }

                throw new UserLinkDuplicateException();
            }

            // UserLink 생성
            UserLink userLink = UserLink.create(user, link, request.why(), request.memo());

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

            // 인덱싱 이벤트 발행 (트랜잭션 커밋 후 비동기 처리)
            eventPublisher.publishEvent(UserLinkCreatedEvent.of(savedUserLink.getId()));

            log.info("사용자 링크 생성 완료 - userId: {}, userLinkId: {}, referenceId: {}, url: {}",
                    userId, savedUserLink.getId(), reference.getId(), request.url());

            return UserLinkResponse.from(savedUserLink, reference);
        });
    }

    /**
     * 사용자 링크 단건 조회 - 소유자만 조회 가능 (N:N 관계로 공개/비공개 복잡도 증가) - 소유자가 조회 시 조회수 증가
     */
    @Transactional
    public UserLinkResponse getUserLink(Long userId, Long userLinkId) {
        UserLink userLink = getVisibleUserLinkById(userLinkId);

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
     * 미리보기 렌더링용 사용자 링크를 조회한다.
     * 조회수는 증가시키지 않으며, 미리보기 표시를 위한 현재 상태만 반환한다.
     */
    @Transactional(readOnly = true)
    public UserLinkListResponse getUserLinkPreview(Long userId, Long userLinkId) {
        UserLink userLink = getVisibleUserLinkById(userLinkId);

        userLink.validateOwner(userId);

        Reference reference = referenceUserLinkRepository.findByUserLinkId(userLinkId).stream()
                .map(ReferenceUserLink::getReference)
                .findFirst()
                .orElse(null);

        return UserLinkListResponse.of(userLink, reference);
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

        boolean chatbotReindexRequired = request.why() != null || request.memo() != null;

        if (chatbotReindexRequired) {
            // why/memo 변경은 챗봇 문서 내용에 직접 반영되므로 별도 이벤트로 분리한다.
            eventPublisher.publishEvent(UserLinkChatbotReindexEvent.of(userLinkId));
        }

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

        // 추천 인덱스 삭제 및 추천 캐시 무효화 이벤트 발행 (트랜잭션 커밋 후 비동기 처리)
        eventPublisher.publishEvent(UserLinkDeletedEvent.of(userLinkId, userId));

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
    @Transactional(readOnly = true)
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
        String nextCursor = hasNext ? String.valueOf(content.getLast().getId()) : null;

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

    private UserLink getVisibleUserLinkById(Long userLinkId) {
        UserLink userLink = getUserLinkById(userLinkId);
        if (!userLink.getLink().isReady()) {
            throw new UserLinkNotFoundException();
        }
        return userLink;
    }

    private Reference getReferenceById(Long referenceId) {
        return referenceRepository.findById(referenceId).orElseThrow(ReferenceNotFoundException::new);
    }
}
