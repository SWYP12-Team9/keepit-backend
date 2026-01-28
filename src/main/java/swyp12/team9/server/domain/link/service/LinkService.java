package swyp12.team9.server.domain.link.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swyp12.team9.server.api.link.dto.CreateLinkRequest;
import swyp12.team9.server.api.link.dto.LinkResponse;
import swyp12.team9.server.api.link.dto.UpdateLinkRequest;
import swyp12.team9.server.domain.link.exception.LinkAccessDeniedException;
import swyp12.team9.server.domain.link.exception.LinkNotFoundException;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.link.repository.LinkRepository;
import swyp12.team9.server.domain.reference.exception.ReferenceNotFoundException;
import swyp12.team9.server.domain.reference.model.Reference;
import swyp12.team9.server.domain.reference.repository.ReferenceRepository;
import swyp12.team9.server.domain.user.exception.UserNotFoundException;
import swyp12.team9.server.domain.user.repository.UserRepository;
import swyp12.team9.server.global.util.PaginationUtils.Cursor.PageResponse;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LinkService {

    private final LinkRepository linkRepository;
    private final ReferenceRepository referenceRepository;
    private final UserRepository userRepository;

    /**
     * 링크 생성
     */
    @Transactional
    public LinkResponse createLink(Long userId, CreateLinkRequest request) {
        // 레퍼런스(폴더) 조회 및 소유자 검증
        Reference reference = referenceRepository.findById(request.referenceId())
                .orElseThrow(() -> new ReferenceNotFoundException("레퍼런스를 찾을 수 없습니다. ID: " + request.referenceId()));

        reference.validateOwner(userId);

        // 링크 생성
        Link link = Link.builder()
                .reference(reference)
                .url(request.url())
                .title(request.title())
                .thumbnailUrl(request.thumbnailUrl())
                .summary(request.summary())
                .why(request.why())
                .memo(request.memo())
                .isPublic(request.isPublic())
                .build();

        Link savedLink = linkRepository.save(link);

        log.info("링크 생성 완료 - userId: {}, linkId: {}, url: {}", userId, savedLink.getId(), request.url());

        return LinkResponse.from(savedLink, List.of());
    }

    /**
     * 링크 단건 조회
     */
    public LinkResponse getLink(Long userId, Long linkId) {
        Link link = getLinkById(linkId);

        // 공개 링크면 누구나 조회 가능
        if (link.getIsPublic()) {
            return LinkResponse.from(link, List.of());
        }

        // 비공개 링크는 소유자만 조회 가능
        if (userId == null) {
            throw new LinkAccessDeniedException("비공개 링크는 로그인이 필요합니다.");
        }

        link.validateOwner(userId);
        return LinkResponse.from(link, List.of());
    }

    /**
     * 링크 수정
     */
    @Transactional
    public LinkResponse updateLink(Long userId, Long linkId, UpdateLinkRequest request) {
        Link link = getLinkById(linkId);

        // 소유자 검증
        link.validateOwner(userId);

        // 링크 정보 수정
        link.update(
                request.title(),
                request.thumbnailUrl(),
                request.summary(),
                request.why(),
                request.memo(),
                request.isPublic()
        );

        log.info("링크 수정 완료 - userId: {}, linkId: {}", userId, linkId);
        return LinkResponse.from(link, List.of());
    }

    /**
     * 링크 삭제
     */
    @Transactional
    public void deleteLink(Long userId, Long linkId) {
        Link link = getLinkById(linkId);

        // 소유자 검증
        link.validateOwner(userId);

        linkRepository.delete(link);

        log.info("링크 삭제 완료 - userId: {}, linkId: {}", userId, linkId);
    }

    /**
     * 링크 열람 처리
     */
    @Transactional
    public LinkResponse markAsViewed(Long userId, Long linkId) {
        Link link = getLinkById(linkId);

        // 소유자 검증
        link.validateOwner(userId);

        link.markAsViewed();

        log.info("링크 열람 처리 완료 - userId: {}, linkId: {}", userId, linkId);
        return LinkResponse.from(link, List.of());
    }

    /**
     * 즐겨찾기 토글
     */
    @Transactional
    public LinkResponse toggleBookmark(Long userId, Long linkId) {
        Link link = getLinkById(linkId);

        // 소유자 검증
        link.validateOwner(userId);

        link.toggleBookmark();

        log.info("즐겨찾기 토글 완료 - userId: {}, linkId: {}, isBookmarked: {}", userId, linkId, link.getIsBookmarked());
        return LinkResponse.from(link, List.of());
    }

    /**
     * 특정 레퍼런스(폴더)의 링크 목록 조회
     */
    public PageResponse<LinkResponse> getLinksByReference(Long userId, Long referenceId, String cursor, int size) {
        Reference reference = referenceRepository.findById(referenceId)
                .orElseThrow(() -> new ReferenceNotFoundException("레퍼런스를 찾을 수 없습니다. ID: " + referenceId));

        // 비공개 레퍼런스는 소유자만 조회 가능
        if (!reference.getIsPublic()) {
            if (userId == null) {
                throw new LinkAccessDeniedException("비공개 레퍼런스는 로그인이 필요합니다.");
            }
            reference.validateOwner(userId);
        }

        PageRequest pageRequest = PageRequest.of(0, size + 1);

        List<Link> links;
        if (cursor == null) {
            links = linkRepository.findByReferenceOrderByIdDesc(reference, pageRequest);
        } else {
            Long cursorId = Long.parseLong(cursor);
            links = linkRepository.findByReferenceAndIdLessThanOrderByIdDesc(reference, cursorId, pageRequest);
        }

        return buildCursorResponse(links, size);
    }

    /**
     * 내 전체 링크 목록 조회
     */
    public PageResponse<LinkResponse> getMyLinks(Long userId, String cursor, int size) {
        if (userId == null) {
            throw new LinkAccessDeniedException("로그인이 필요합니다.");
        }

        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId);
        }

        PageRequest pageRequest = PageRequest.of(0, size + 1);

        List<Link> links;
        if (cursor == null) {
            links = linkRepository.findByReference_User_IdOrderByIdDesc(userId, pageRequest);
        } else {
            Long cursorId = Long.parseLong(cursor);
            links = linkRepository.findByReference_User_IdAndIdLessThanOrderByIdDesc(userId, cursorId, pageRequest);
        }

        return buildCursorResponse(links, size);
    }

    /**
     * 공개 링크 목록 조회
     */
    public PageResponse<LinkResponse> getPublicLinks(String cursor, int size) {
        PageRequest pageRequest = PageRequest.of(0, size + 1);

        List<Link> links;
        if (cursor == null) {
            links = linkRepository.findByIsPublicTrueOrderByIdDesc(pageRequest);
        } else {
            Long cursorId = Long.parseLong(cursor);
            links = linkRepository.findByIsPublicTrueAndIdLessThanOrderByIdDesc(cursorId, pageRequest);
        }

        return buildCursorResponse(links, size);
    }

    /**
     * 탐색(Explore) - 카테고리별 공개 링크 목록 조회
     */
    public PageResponse<LinkResponse> getPublicLinksByCategory(Long categoryId, String cursor, int size) {
        if (categoryId != null) {
            log.warn("카테고리 기능은 별도 브랜치에서 제공됩니다. categoryId={}", categoryId);
        }
        return getPublicLinks(cursor, size);
    }

    /**
     * 즐겨찾기 링크 목록 조회
     */
    public PageResponse<LinkResponse> getBookmarkedLinks(Long userId, String cursor, int size) {
        if (userId == null) {
            throw new LinkAccessDeniedException("로그인이 필요합니다.");
        }

        PageRequest pageRequest = PageRequest.of(0, size + 1);

        List<Link> links;
        if (cursor == null) {
            links = linkRepository.findByReference_User_IdAndIsBookmarkedTrueOrderByIdDesc(userId, pageRequest);
        } else {
            Long cursorId = Long.parseLong(cursor);
            links = linkRepository.findByReference_User_IdAndIsBookmarkedTrueAndIdLessThanOrderByIdDesc(userId, cursorId, pageRequest);
        }

        return buildCursorResponse(links, size);
    }

    // ========== Helper 메서드 ==========

    private Link getLinkById(Long linkId) {
        return linkRepository.findById(linkId)
                .orElseThrow(() -> new LinkNotFoundException("링크를 찾을 수 없습니다. ID: " + linkId));
    }

    private PageResponse<LinkResponse> buildCursorResponse(List<Link> links, int size) {
        if (links.isEmpty()) {
            return PageResponse.empty();
        }

        boolean hasNext = links.size() > size;
        List<Link> content = hasNext ? links.subList(0, size) : links;
        String nextCursor = hasNext ? String.valueOf(content.get(content.size() - 1).getId()) : null;

        List<LinkResponse> responses = content.stream()
                .map(link -> LinkResponse.from(link, List.of()))
                .collect(Collectors.toList());

        return PageResponse.of(responses, nextCursor, hasNext);
    }
}
