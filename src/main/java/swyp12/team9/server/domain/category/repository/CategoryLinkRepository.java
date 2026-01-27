package swyp12.team9.server.domain.category.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swyp12.team9.server.domain.category.model.Category;
import swyp12.team9.server.domain.category.model.CategoryLink;
import swyp12.team9.server.domain.link.model.Link;

import java.util.List;

@Repository
public interface CategoryLinkRepository extends JpaRepository<CategoryLink, Long> {

    /**
     * 특정 링크의 카테고리 목록 조회
     */
    List<CategoryLink> findByLink(Link link);

    /**
     * 특정 링크의 카테고리 ID 목록으로 조회
     */
    List<CategoryLink> findByLinkId(Long linkId);

    /**
     * 특정 카테고리에 속한 공개 링크 목록 조회 (커서 페이징 - 첫 페이지)
     */
    List<CategoryLink> findByCategoryAndLink_IsPublicTrueOrderByLink_IdDesc(Category category, Pageable pageable);

    /**
     * 특정 카테고리에 속한 공개 링크 목록 조회 (커서 페이징 - 다음 페이지)
     */
    List<CategoryLink> findByCategoryAndLink_IsPublicTrueAndLink_IdLessThanOrderByLink_IdDesc(
            Category category, Long cursor, Pageable pageable);

    /**
     * 링크와 카테고리로 존재 여부 확인
     */
    boolean existsByLinkAndCategory(Link link, Category category);

    /**
     * 특정 링크의 카테고리 연결 삭제
     */
    void deleteByLink(Link link);

    /**
     * 특정 링크와 카테고리 연결 삭제
     */
    void deleteByLinkAndCategory(Link link, Category category);
}
