package swyp12.team9.server.domain.link.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swyp12.team9.server.domain.link.model.Link;

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
}
