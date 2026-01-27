package swyp12.team9.server.domain.link.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.link.model.ViewStatus;
import swyp12.team9.server.domain.reference.model.Reference;

import java.util.List;
import java.util.Optional;

@Repository
public interface LinkRepository extends JpaRepository<Link, Long> {

    /**
     * URL로 Link 조회
     */
    Optional<Link> findByUrl(String url);

    /**
     * URL 존재 여부 확인
     */
    boolean existsByUrl(String url);

    /**
     * 특정 레퍼런스(폴더)의 링크 목록 조회
     */
    List<Link> findByReference(Reference reference);

    /**
     * 특정 레퍼런스(폴더)의 링크 목록 조회 (커서 페이징 - 첫 페이지)
     */
    List<Link> findByReferenceOrderByIdDesc(Reference reference, Pageable pageable);

    /**
     * 특정 레퍼런스(폴더)의 링크 목록 조회 (커서 페이징 - 다음 페이지)
     */
    List<Link> findByReferenceAndIdLessThanOrderByIdDesc(Reference reference, Long cursor, Pageable pageable);

    /**
     * 특정 사용자의 모든 링크 조회 (레퍼런스 소유자 기준, 커서 페이징 - 첫 페이지)
     */
    List<Link> findByReference_User_IdOrderByIdDesc(Long userId, Pageable pageable);

    /**
     * 특정 사용자의 모든 링크 조회 (레퍼런스 소유자 기준, 커서 페이징 - 다음 페이지)
     */
    List<Link> findByReference_User_IdAndIdLessThanOrderByIdDesc(Long userId, Long cursor, Pageable pageable);

    /**
     * 공개 링크 목록 조회 (커서 페이징 - 첫 페이지)
     */
    List<Link> findByIsPublicTrueOrderByIdDesc(Pageable pageable);

    /**
     * 공개 링크 목록 조회 (커서 페이징 - 다음 페이지)
     */
    List<Link> findByIsPublicTrueAndIdLessThanOrderByIdDesc(Long cursor, Pageable pageable);

    /**
     * 특정 사용자의 비공개 링크 목록 조회 (커서 페이징 - 첫 페이지)
     */
    List<Link> findByReference_User_IdAndIsPublicFalseOrderByIdDesc(Long userId, Pageable pageable);

    /**
     * 특정 사용자의 비공개 링크 목록 조회 (커서 페이징 - 다음 페이지)
     */
    List<Link> findByReference_User_IdAndIsPublicFalseAndIdLessThanOrderByIdDesc(Long userId, Long cursor, Pageable pageable);

    /**
     * 즐겨찾기한 링크 목록 조회 (커서 페이징 - 첫 페이지)
     */
    List<Link> findByReference_User_IdAndIsBookmarkedTrueOrderByIdDesc(Long userId, Pageable pageable);

    /**
     * 즐겨찾기한 링크 목록 조회 (커서 페이징 - 다음 페이지)
     */
    List<Link> findByReference_User_IdAndIsBookmarkedTrueAndIdLessThanOrderByIdDesc(Long userId, Long cursor, Pageable pageable);

    /**
     * 열람 상태별 링크 목록 조회 (커서 페이징 - 첫 페이지)
     */
    List<Link> findByReference_User_IdAndViewStatusOrderByIdDesc(Long userId, ViewStatus viewStatus, Pageable pageable);

    /**
     * 열람 상태별 링크 목록 조회 (커서 페이징 - 다음 페이지)
     */
    List<Link> findByReference_User_IdAndViewStatusAndIdLessThanOrderByIdDesc(
            Long userId, ViewStatus viewStatus, Long cursor, Pageable pageable);

    /**
     * 특정 레퍼런스와 URL로 중복 확인
     */
    boolean existsByReferenceAndUrl(Reference reference, String url);
}
