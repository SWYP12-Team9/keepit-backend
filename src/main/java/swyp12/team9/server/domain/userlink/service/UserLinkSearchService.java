package swyp12.team9.server.domain.userlink.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swyp12.team9.server.api.userlink.dto.response.UserLinkSearchResponse;
import swyp12.team9.server.domain.reference.exception.ReferenceNotFoundException;
import swyp12.team9.server.domain.reference.model.Reference;
import swyp12.team9.server.domain.reference.repository.ReferenceRepository;
import swyp12.team9.server.domain.userlink.model.UserLink;
import swyp12.team9.server.domain.userlink.repository.UserLinkSearchRepository;

/**
 * 링크 검색 서비스 (커서 기반 페이징)
 * <p>
 * QueryDSL을 활용한 동적 검색 쿼리를 제공합니다.
 * <p>
 * 검색 대상 필드: - UserLink.why (저장 이유) - UserLink.memo (메모) - Link.title (링크 제목) - Link.aiSummary (AI 요약) - Link.url (링크
 * URL)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserLinkSearchService {

    private final UserLinkSearchRepository userLinkSearchRepository;
    private final ReferenceRepository referenceRepository;

    /**
     * 내 링크에서 키워드 검색 (커서 기반)
     *
     * @param userId  검색할 사용자 ID
     * @param keyword 검색 키워드
     * @param field   검색할 필드 (null이면 전체 필드)
     * @param cursor  커서 (null이면 첫 페이지)
     * @param size    페이지 크기
     * @return 검색 결과 (커서 페이징)
     */
    public UserLinkSearchResponse searchMyLinks(Long userId, String keyword, String field, String cursor, int size) {

        String normalizedKeyword = (keyword == null) ? "" : keyword.trim();
        if (normalizedKeyword.isEmpty()) {
            log.warn("검색어가 비어있습니다. userId: {}", userId);
            return UserLinkSearchResponse.empty();
        }

        Long cursorId = parseCursor(cursor);

        log.info("내 링크 검색 - userId: {}, keyword: {}, field: {}, cursor: {}",
                userId, normalizedKeyword, field, cursorId);

        // size + 1개를 조회하여 다음 페이지 존재 여부 확인
        List<UserLink> result = userLinkSearchRepository.searchMyLinks(
                userId, normalizedKeyword, field, cursorId, size + 1
        );

        log.info("검색 완료 - 결과: {}건", Math.min(result.size(), size));

        return UserLinkSearchResponse.from(result, normalizedKeyword, size);
    }

    /**
     * 레퍼런스 폴더 내 링크 검색 (커서 기반)
     *
     * @param userId      현재 로그인한 사용자 ID
     * @param referenceId 검색할 레퍼런스 폴더 ID
     * @param keyword     검색 키워드
     * @param field       검색할 필드 (null이면 전체 필드)
     * @param cursor      커서 (null이면 첫 페이지)
     * @param size        페이지 크기
     * @return 검색 결과 (커서 페이징)
     */
    public UserLinkSearchResponse searchByReference(Long userId, Long referenceId, String keyword, String field,
                                                    String cursor, int size) {
        // 레퍼런스 조회 및 소유자 검증
        Reference reference = referenceRepository.findById(referenceId)
                .orElseThrow(ReferenceNotFoundException::new);
        reference.validateOwner(userId);

        if (keyword == null || keyword.trim().isEmpty()) {
            log.warn("검색어가 비어있습니다. referenceId: {}", referenceId);
            return UserLinkSearchResponse.empty();
        }

        String normalizedKeyword = keyword.trim();
        Long cursorId = parseCursor(cursor);

        log.info("레퍼런스 폴더 내 검색 - referenceId: {}, keyword: {}, field: {}, cursor: {}",
                referenceId, normalizedKeyword, field, cursorId);

        List<UserLink> result = userLinkSearchRepository.searchByReference(
                referenceId, normalizedKeyword, field, cursorId, size + 1
        );

        log.info("레퍼런스 폴더 검색 완료 - 결과: {}건", Math.min(result.size(), size));

        return UserLinkSearchResponse.from(result, normalizedKeyword, size);
    }

    /**
     * 커서 문자열을 Long으로 파싱
     *
     * @param cursor 커서 문자열
     * @return 파싱된 Long 값 (null이면 null 반환)
     */
    private Long parseCursor(String cursor) {
        if (cursor == null || cursor.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(cursor);
        } catch (NumberFormatException e) {
            log.warn("잘못된 커서 형식: {}", cursor);
            return null;
        }
    }
}
