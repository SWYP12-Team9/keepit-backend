package swyp12.team9.server.domain.link.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swyp12.team9.server.api.link.dto.CreateLinkRequest;
import swyp12.team9.server.api.link.dto.LinkResponse;
import swyp12.team9.server.api.link.dto.UpdateLinkRequest;
import swyp12.team9.server.domain.category.model.Category;
import swyp12.team9.server.domain.category.model.CategoryLink;
import swyp12.team9.server.domain.category.repository.CategoryLinkRepository;
import swyp12.team9.server.domain.category.repository.CategoryRepository;
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
    private final CategoryRepository categoryRepository;
    private final CategoryLinkRepository categoryLinkRepository;
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

        // 카테고리 연결 (탐색 탭용)
        if (request.categoryIds() != null && !request.categoryIds().isEmpty()) {
            for (Long categoryId : request.categoryIds()) {
                Category category = categoryRepository.findById(categoryId)
                        .orElse(null);
                if (category != null) {
                    CategoryLink categoryLink = CategoryLink.builder()
                            .link(savedLink)
                            .category(category)
                            .build();
                    categoryLinkRepository.save(categoryLink);
                }
            }
        }

        log.info("링크 생성 완료 - userId: {}, linkId: {}, url: {}", userId, savedLink.getId(), request.url());

        return LinkResponse.from(savedLink, getCategoryNames(savedLink));
    }

    /**
     * 링크 단건 조회
     */
    public LinkResponse getLink(Long userId, Long linkId) {
        Link link = getLinkById(linkId);

        // 공개 링크면 누구나 조회 가능
        if (link.getIsPublic()) {
            return LinkResponse.from(link, getCategoryNames(link));
        }

        // 비공개 링크는 소유자만 조회 가능
        if (userId == null) {
            throw new LinkAccessDeniedException("비공개 링크는 로그인이 필요합니다.");
        }

        link.validateOwner(userId);
        return LinkResponse.from(link, getCategoryNames(link));
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

        // 카테고리 업데이트 (기존 연결 삭제 후 새로 연결)
        if (request.categoryIds() != null) {
            categoryLinkRepository.deleteByLink(link);
            for (Long categoryId : request.categoryIds()) {
                Category category = categoryRepository.findById(categoryId).orElse(null);
                if (category != null) {
                    CategoryLink categoryLink = CategoryLink.builder()
                            .link(link)
                            .category(category)
                            .build();
                    categoryLinkRepository.save(categoryLink);
                }
            }
        }

        log.info("링크 수정 완료 - userId: {}, linkId: {}", userId, linkId);
        return LinkResponse.from(link, getCategoryNames(link));
    }

    /**
     * 링크 삭제
     */
    @Transactional
    public void deleteLink(Long userId, Long linkId) {
        Link link = getLinkById(linkId);

        // 소유자 검증
        link.validateOwner(userId);

        // 카테고리 연결 먼저 삭제
        categoryLinkRepository.deleteByLink(link);

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
        return LinkResponse.from(link, getCategoryNames(link));
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
        return LinkResponse.from(link, getCategoryNames(link));
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
        // 카테고리 ID가 없으면 전체 공개 링크 조회
        if (categoryId == null) {
            return getPublicLinks(cursor, size);
        }

        Category category = categoryRepository.findById(categoryId)
                .orElse(null);

        if (category == null) {
            log.warn("잘못된 카테고리 ID: {}", categoryId);
            return PageResponse.empty();
        }

        PageRequest pageRequest = PageRequest.of(0, size + 1);

        List<CategoryLink> categoryLinks;
        if (cursor == null) {
            categoryLinks = categoryLinkRepository.findByCategoryAndLink_IsPublicTrueOrderByLink_IdDesc(category, pageRequest);
        } else {
            Long cursorId = Long.parseLong(cursor);
            categoryLinks = categoryLinkRepository.findByCategoryAndLink_IsPublicTrueAndLink_IdLessThanOrderByLink_IdDesc(
                    category, cursorId, pageRequest);
        }

        List<Link> links = categoryLinks.stream()
                .map(CategoryLink::getLink)
                .collect(Collectors.toList());

        return buildCursorResponse(links, size);
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

    private List<String> getCategoryNames(Link link) {
        return categoryLinkRepository.findByLinkId(link.getId()).stream()
                .map(cl -> cl.getCategory().getName())
                .collect(Collectors.toList());
    }

    private PageResponse<LinkResponse> buildCursorResponse(List<Link> links, int size) {
        if (links.isEmpty()) {
            return PageResponse.empty();
        }

        boolean hasNext = links.size() > size;
        List<Link> content = hasNext ? links.subList(0, size) : links;
        String nextCursor = hasNext ? String.valueOf(content.get(content.size() - 1).getId()) : null;

        List<LinkResponse> responses = content.stream()
                .map(link -> LinkResponse.from(link, getCategoryNames(link)))
                .collect(Collectors.toList());

        return PageResponse.of(responses, nextCursor, hasNext);
    }
}
