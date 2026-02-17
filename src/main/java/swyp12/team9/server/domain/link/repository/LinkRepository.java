package swyp12.team9.server.domain.link.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swyp12.team9.server.domain.link.model.Link;

import java.util.List;
import java.util.Optional;

@Repository
public interface LinkRepository extends JpaRepository<Link, Long> {

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
}
