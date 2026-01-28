package swyp12.team9.server.domain.bookmark.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swyp12.team9.server.domain.bookmark.model.Bookmark;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    /**
     * 특정 Link의 북마크 수 조회
     */
    long countByLinkId(Long linkId);
}
