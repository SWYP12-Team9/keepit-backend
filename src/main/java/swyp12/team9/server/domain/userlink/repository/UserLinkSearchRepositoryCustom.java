package swyp12.team9.server.domain.userlink.repository;

import java.util.List;
import swyp12.team9.server.domain.userlink.model.UserLink;

/**
 * UserLink 검색 Repository Custom 인터페이스
 * <p>
 * QueryDSL을 활용한 동적 검색 쿼리를 정의합니다.
 */
public interface UserLinkSearchRepositoryCustom {

    /**
     * 내 링크에서 키워드 검색 (커서 기반)
     *
     * @param userId   사용자 ID
     * @param keyword  검색 키워드
     * @param field    검색 필드 (null이면 전체 필드)
     * @param cursorId 커서 ID (null이면 첫 페이지)
     * @param size     조회할 개수
     * @return 검색된 UserLink 목록
     */
    List<UserLink> searchMyLinks(Long userId, String keyword, String field, Long cursorId, int size);

    /**
     * 특정 레퍼런스 폴더 내 링크 검색 (커서 기반)
     *
     * @param referenceId 레퍼런스 ID
     * @param keyword     검색 키워드
     * @param field       검색 필드 (null이면 전체 필드)
     * @param cursorId    커서 ID (null이면 첫 페이지)
     * @param size        조회할 개수
     * @return 검색된 UserLink 목록
     */
    List<UserLink> searchByReference(Long referenceId, String keyword, String field, Long cursorId, int size);

    /**
     * 내 모든 레퍼런스 폴더 내 링크 검색 (커서 기반)
     *
     * @param userId   사용자 ID
     * @param keyword  검색 키워드
     * @param field    검색 필드 (null이면 전체 필드)
     * @param cursorId 커서 ID (null이면 첫 페이지)
     * @param size     조회할 개수
     * @return 검색된 UserLink 목록 (중복 제거)
     */
    List<UserLink> searchAllMyReferences(Long userId, String keyword, String field, Long cursorId, int size);
}
