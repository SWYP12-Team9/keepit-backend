package swyp12.team9.server.domain.link.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.link.model.LinkProcessingStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface LinkRepository extends JpaRepository<Link, Long> {

    // 일정 시간 넘게 PENDING에 머문 링크를 ID 순으로 페이지 단위 조회 (Stream 등록이 유실됐을 가능성)
    // ponytail: (processing_status, updated_at) 인덱스 없음. links가 커져 이 조회가 느려지면 인덱스 추가
    List<Link> findByProcessingStatusAndUpdatedAtBeforeAndIdGreaterThanOrderByIdAsc(
            LinkProcessingStatus processingStatus, LocalDateTime updatedBefore, Long idAfter, Pageable pageable);

    /**
     * URL 해시로 Link 조회 (url_hash UNIQUE 인덱스 기반)
     */
    Optional<Link> findByUrlHash(String urlHash);

    /**
     * URL 해시 존재 여부 확인
     */
    boolean existsByUrlHash(String urlHash);

    /**
     * ID 목록에 해당하지 않는 링크 조회 (추천용)
     */
    List<Link> findByIdNotInOrderByIdDesc(List<Long> excludeIds, Pageable pageable);

    /**
     * 전체 링크 조회 (최신순, 페이징)
     */
    List<Link> findAllByOrderByIdDesc(Pageable pageable);

    /**
     * 공개 조회수 1 증가 (원자적)
     */
    @Modifying
    @Query("UPDATE Link l SET l.publicViewCount = l.publicViewCount + 1 WHERE l.id = :linkId")
    int incrementPublicViewCount(@Param("linkId") Long linkId);

    @Modifying
    @Query("UPDATE Link l SET l.publicViewCount = l.publicViewCount + :increment WHERE l.id = :id")
    int incrementPublicViewCountById(@Param("id") Long id, @Param("increment") long increment);

    default void batchIncrementPublicViewCount(Map<Long, Long> countMap) {
        countMap.forEach(this::incrementPublicViewCountById);
    }
}
