package swyp12.team9.server.domain.userlink.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swyp12.team9.server.domain.userlink.model.UserLink;

/**
 * UserLink 검색 Repository
 * <p>
 * JpaRepository + QueryDSL Custom 구현을 결합하여 사용합니다.
 * <p>
 * 검색 기능: - searchMyLinks: 내 링크에서 검색 - searchByReference: 특정 레퍼런스 폴더 내 검색 - searchAllMyReferences: 내 모든 레퍼런스 폴더 내 검색
 * <p>
 * 모든 검색은 커서 기반 페이징을 사용합니다.
 */
@Repository
public interface UserLinkSearchRepository extends JpaRepository<UserLink, Long>, UserLinkSearchRepositoryCustom {
    // QueryDSL 구현체(UserLinkSearchRepositoryImpl)에서 모든 검색 메서드 제공
}