package swyp12.team9.server.domain.reference.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swyp12.team9.server.domain.reference.model.Reference;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReferenceRepository extends JpaRepository<Reference, Long>, ReferenceRepositoryCustom {

    // ========== 기본 조회 ==========
    // 사용자 ID로 레퍼런스 목록 조회
    List<Reference> findByUserId(Long userId);

    // 공개 레퍼런스 목록 조회
    List<Reference> findByIsPublicTrue();

    // 사용자 ID와 공개 여부로 조회
    List<Reference> findByUserIdAndIsPublic(Long userId, Boolean isPublic);

    // 사용자가 해당 제목의 레퍼런스를 이미 가지고 있는지 확인
    boolean existsByUserIdAndTitle(Long userId, String title);

    // 사용자의 기본 미지정 폴더 조회
    Optional<Reference> findByUserIdAndIsDefaultTrue(Long userId);

    // ========== 커서 페이징: 내 레퍼런스 전체 ==========
    /**
     * 사용자 레퍼런스 커서 페이징 (첫 페이지)
     */
    List<Reference> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);

    /**
     * 사용자 레퍼런스 커서 페이징 (다음 페이지)
     */
    List<Reference> findByUserIdAndIdLessThanOrderByIdDesc(Long userId, Long cursor, Pageable pageable);

    // ========== 커서 페이징: 공개 레퍼런스 ==========
    /**
     * 공개 레퍼런스 커서 페이징 (첫 페이지)
     */
    List<Reference> findByIsPublicTrueOrderByIdDesc(Pageable pageable);

    /**
     * 공개 레퍼런스 커서 페이징 (다음 페이지)
     */
    List<Reference> findByIsPublicTrueAndIdLessThanOrderByIdDesc(Long cursor, Pageable pageable);


    // ========== 커서 페이징: 비공개 레퍼런스 ==========
    /**
     * 특정 사용자의 비공개 레퍼런스 커서 페이징 (첫 페이지)
     */
    List<Reference> findByUserIdAndIsPublicFalseOrderByIdDesc(Long userId, Pageable pageable);

    /**
     * 특정 사용자의 비공개 레퍼런스 커서 페이징 (다음 페이지)
     */
    List<Reference> findByUserIdAndIsPublicFalseAndIdLessThanOrderByIdDesc(
            Long userId, Long cursor, Pageable pageable);


    // ========== 커서 페이징: 비공개 레퍼런스 (관리자 전용) ==========
    /**
     * [관리자] 모든 비공개 레퍼런스 커서 페이징 (첫 페이지)
     * 모든 사용자의 비공개 레퍼런스를 조회
     */
    List<Reference> findByIsPublicFalseOrderByIdDesc(Pageable pageable);

    /**
     * [관리자] 모든 비공개 레퍼런스 커서 페이징 (다음 페이지)
     * cursor 이후의 모든 비공개 레퍼런스를 조회
     */
    List<Reference> findByIsPublicFalseAndIdLessThanOrderByIdDesc(Long cursor, Pageable pageable);
}
