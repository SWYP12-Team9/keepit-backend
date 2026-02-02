package swyp12.team9.server.domain.userlink.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swyp12.team9.server.api.userlink.dto.request.UserLinkCreateRequest;
import swyp12.team9.server.api.userlink.dto.request.UserLinkUpdateRequest;
import swyp12.team9.server.api.userlink.dto.response.ReferenceInfo;
import swyp12.team9.server.api.userlink.dto.response.UserLinkListResponse;
import swyp12.team9.server.api.userlink.dto.response.UserLinkResponse;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.link.repository.LinkRepository;
import swyp12.team9.server.domain.reference.exception.ReferenceNotFoundException;
import swyp12.team9.server.domain.reference.model.Reference;
import swyp12.team9.server.domain.reference.repository.ReferenceRepository;
import swyp12.team9.server.domain.reference.service.ReferenceService;
import swyp12.team9.server.domain.referenceuserlink.model.ReferenceUserLink;
import swyp12.team9.server.domain.referenceuserlink.repository.ReferenceUserLinkRepository;
import swyp12.team9.server.domain.user.exception.UserNotFoundException;
import swyp12.team9.server.domain.user.model.User;
import swyp12.team9.server.domain.user.repository.UserRepository;
import swyp12.team9.server.domain.userlink.exception.UserLinkAccessDeniedException;
import swyp12.team9.server.domain.userlink.exception.UserLinkDuplicateException;
import swyp12.team9.server.domain.userlink.exception.UserLinkNotFoundException;
import swyp12.team9.server.domain.userlink.model.UserLink;
import swyp12.team9.server.domain.userlink.repository.UserLinkRepository;
import swyp12.team9.server.global.util.PaginationUtils.Cursor.PageResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserLinkService {

    private final UserLinkRepository userLinkRepository;
    private final LinkRepository linkRepository;
//    private final LinkService linkService; TODO 박현제: 스크래핑 로직 추가 예정
    private final UserRepository userRepository;
    private final ReferenceRepository referenceRepository;
    private final ReferenceUserLinkRepository referenceUserLinkRepository;
    private final ReferenceService referenceService;

    /**
     * 사용자 링크 생성
     * - 중복 체크를 먼저 수행한 후 Link 생성
     * - LinkService를 통해 Link 스크래핑 로직 처리
     * - referenceIds가 null이거나 빈 배열이면 기본 미지정 폴더에 자동 분류
     * - 여러 Reference에 동시에 속할 수 있음 (N:N 관계)
     */
    @Transactional
    public UserLinkResponse createUserLink(Long userId, UserLinkCreateRequest request) {
        User user = getUserById(userId);

        // URL로 기존 Link 조회
        Link link = linkRepository.findByUrl(request.url()).orElse(null);

        // Link가 이미 존재하는 경우, 중복 체크 먼저 수행
        if (link != null) {
            if (userLinkRepository.existsByUserIdAndLinkId(userId, link.getId())) {
                throw new UserLinkDuplicateException();
            }
        } else {
            // Link가 없는 경우에만 스크래핑하여 새로 생성
            Link newLink = Link.builder() // TODO 박현제: 임시. 이후 스크래핑 데이터로 변경 예정
                    .url(request.url())
                    .build();
            link = linkRepository.save(newLink);
//            link = linkService.createLinkFromScraping(request.url());
        }

        // UserLink 생성
        UserLink userLink = UserLink.builder()
                .user(user)
                .link(link)
                .why(request.why())
                .memo(request.memo())
                .build();

        UserLink savedUserLink = userLinkRepository.save(userLink);

        // ReferenceUserLink 생성
        List<Reference> references = new ArrayList<>();
        if (request.referenceIds() == null || request.referenceIds().isEmpty()) {
            // referenceIds가 null이거나 빈 배열이면 기본 미지정 폴더에 자동 분류
            Reference defaultReference = referenceService.getOrCreateDefaultReference(userId);
            references.add(defaultReference);
        } else {
            // referenceIds로 Reference 조회 및 검증
            for (Long referenceId : request.referenceIds()) {
                Reference reference = getReferenceById(referenceId);
                reference.validateOwner(userId); // 소유자 검증
                references.add(reference);
            }
        }

        // ReferenceUserLink 엔티티 생성 및 저장
        for (Reference reference : references) {
            ReferenceUserLink referenceUserLink = ReferenceUserLink.builder()
                    .reference(reference)
                    .userLink(savedUserLink)
                    .build();
            referenceUserLinkRepository.save(referenceUserLink);
        }

        log.info("사용자 링크 생성 완료 - userId: {}, userLinkId: {}, referenceCount: {}, url: {}",
                userId, savedUserLink.getId(), references.size(), request.url());

        return UserLinkResponse.from(savedUserLink, references);
    }

    /**
     * 사용자 링크 단건 조회
     * - 소유자만 조회 가능 (N:N 관계로 공개/비공개 복잡도 증가)
     * - 소유자가 조회 시 조회수 증가
     */
    @Transactional
    public UserLinkResponse getUserLink(Long userId, Long userLinkId) {
        UserLink userLink = getUserLinkById(userLinkId);

        // 소유자 검증
        if (userId == null) {
            throw new UserLinkAccessDeniedException("링크 조회는 로그인이 필요합니다.");
        }
        userLink.validateOwner(userId);

        // 조회수 증가
        userLink.incrementViewCount();
        log.debug("조회수 증가 - userLinkId: {}, viewCount: {}", userLinkId, userLink.getViewCount());

        // UserLink에 연결된 Reference 목록 조회
        List<ReferenceUserLink> referenceUserLinks = referenceUserLinkRepository.findByUserLinkId(userLinkId);
        List<Reference> references = referenceUserLinks.stream()
                .map(ReferenceUserLink::getReference)
                .collect(Collectors.toList());

        return UserLinkResponse.from(userLink, references);
    }

    /**
     * 사용자 링크 수정
     * - referenceIds가 제공되면 기존 ReferenceUserLink를 모두 삭제하고 새로 생성
     */
    @Transactional
    public UserLinkResponse updateUserLink(Long userId, Long userLinkId, UserLinkUpdateRequest request) {
        UserLink userLink = getUserLinkById(userLinkId);

        // 소유자 검증
        userLink.validateOwner(userId);

        // 수정 (null이면 기존값 유지)
        userLink.updateUserLink(
                request.why() != null ? request.why() : userLink.getWhy(),
                request.memo() != null ? request.memo() : userLink.getMemo()
        );

        // Reference 처리 (referenceIds가 제공된 경우에만)
        List<Reference> references;
        if (request.referenceIds() != null) {
            // 기존 ReferenceUserLink 모두 삭제
            referenceUserLinkRepository.deleteByUserLinkId(userLinkId);

            // 새로운 ReferenceUserLink 생성
            List<Reference> newReferences = new ArrayList<>();
            if (request.referenceIds().isEmpty()) {
                // 빈 배열이면 기본 미지정 폴더로 이동
                Reference defaultReference = referenceService.getOrCreateDefaultReference(userId);
                newReferences.add(defaultReference);
            } else {
                // referenceIds로 Reference 조회 및 검증
                for (Long referenceId : request.referenceIds()) {
                    Reference reference = getReferenceById(referenceId);
                    reference.validateOwner(userId);
                    newReferences.add(reference);
                }
            }

            // ReferenceUserLink 엔티티 생성 및 저장
            for (Reference reference : newReferences) {
                ReferenceUserLink referenceUserLink = ReferenceUserLink.builder()
                        .reference(reference)
                        .userLink(userLink)
                        .build();
                referenceUserLinkRepository.save(referenceUserLink);
            }

            references = newReferences;
        } else {
            // referenceIds가 null이면 기존 Reference 유지
            List<ReferenceUserLink> referenceUserLinks = referenceUserLinkRepository.findByUserLinkId(userLinkId);
            references = referenceUserLinks.stream()
                    .map(ReferenceUserLink::getReference)
                    .collect(Collectors.toList());
        }

        log.info("사용자 링크 수정 완료 - userId: {}, userLinkId: {}, referenceCount: {}",
                userId, userLinkId, references.size());

        return UserLinkResponse.from(userLink, references);
    }

    /**
     * 사용자 링크 삭제
     * - ReferenceUserLink도 함께 삭제
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
     * 사용자 링크 읽음 처리
     */
    @Transactional
    public UserLinkResponse markAsRead(Long userId, Long userLinkId) {
        UserLink userLink = getUserLinkById(userLinkId);

        // 소유자 검증
        userLink.validateOwner(userId);

        userLink.markAsRead();

        // UserLink에 연결된 Reference 목록 조회
        List<ReferenceUserLink> referenceUserLinks = referenceUserLinkRepository.findByUserLinkId(userLinkId);
        List<Reference> references = referenceUserLinks.stream()
                .map(ReferenceUserLink::getReference)
                .collect(Collectors.toList());

        log.info("사용자 링크 읽음 처리 완료 - userId: {}, userLinkId: {}", userId, userLinkId);

        return UserLinkResponse.from(userLink, references);
    }

    /**
     * 사용자 링크 목록 조회 (커서 페이징)
     *
     * @param userId 현재 사용자 ID
     * @param referenceId 레퍼런스 ID (null이면 전체 조회, 값이 있으면 특정 레퍼런스 조회)
     * @param cursor 커서 (null이면 첫 페이지)
     * @param size 페이지 크기
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
     * @param targetReference 조회한 레퍼런스 (null이면 모든 레퍼런스 포함)
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
                    List<Reference> references;
                    if (targetReference != null) {
                        // referenceId로 조회한 경우, 해당 Reference만 포함
                        references = List.of(targetReference);
                    } else {
                        // 전체 조회인 경우, UserLink에 연결된 모든 Reference 포함
                        List<ReferenceUserLink> referenceUserLinks = referenceUserLinkRepository.findByUserLinkId(userLink.getId());
                        references = referenceUserLinks.stream()
                                .map(ReferenceUserLink::getReference)
                                .collect(Collectors.toList());
                    }

                    return UserLinkListResponse.of(userLink, references);
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
