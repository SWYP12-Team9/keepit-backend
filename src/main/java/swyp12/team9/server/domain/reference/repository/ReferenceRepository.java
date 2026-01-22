package swyp12.team9.server.domain.reference.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swyp12.team9.server.domain.reference.model.Reference;

import java.util.List;

@Repository
public interface ReferenceRepository extends JpaRepository<Reference, Long> {

    // 사용자 ID로 레퍼런스 목록 조회
    List<Reference> findByUserId(Long userId);

    // 공개 레퍼런스 목록 조회
    List<Reference> findByIsPublicTrue();

    // 비공개 레퍼런스 목록 조회
    List<Reference> findByIsPublicFalse();

    // 사용자 ID와 공개 여부로 조회
    List<Reference> findByUserIdAndIsPublic(Long userId, Boolean isPublic);

    /**
     * 사용자 레퍼런스 커서 페이징 (첫 페이지)
     */
    List<Reference> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);

    /**
     * 사용자 레퍼런스 커서 페이징 (다음 페이지)
     */
    List<Reference> findByUserIdAndIdLessThanOrderByIdDesc(Long userId, Long cursor, Pageable pageable);

    /**
     * 공개 레퍼런스 커서 페이징 (첫 페이지)
     */
    List<Reference> findByIsPublicTrueOrderByIdDesc(Pageable pageable);

    /**
     * 공개 레퍼런스 커서 페이징 (다음 페이지)
     */
    List<Reference> findByIsPublicTrueAndIdLessThanOrderByIdDesc(Long cursor, Pageable pageable);

    /**
     * 비공개 레퍼런스 커서 페이징 (첫 페이지)
     */
    List<Reference> findByIsPublicFalseOrderByIdDesc(Pageable pageable);

    /**
     * 비공개 레퍼런스 커서 페이징 (다음 페이지)
     */
    List<Reference> findByIsPublicFalseAndIdLessThanOrderByIdDesc(Long cursor, Pageable pageable);

}
