package swyp12.team9.server.domain.userlink.repository;

import org.springframework.data.domain.Pageable;
import swyp12.team9.server.domain.userlink.dto.DayCountProjection;
import swyp12.team9.server.domain.userlink.dto.StatusCountProjection;
import swyp12.team9.server.domain.userlink.model.LinkStatus;
import swyp12.team9.server.domain.userlink.model.UserLink;

import java.time.LocalDateTime;
import java.util.List;

/**
 * UserLink Repository Custom 인터페이스
 */
public interface UserLinkRepositoryCustom {
    /**
     * UserLink 목록 조회 (커서 페이징)
     * 
     * @param userId      사용자 ID
     * @param referenceId 레퍼런스 ID (null이면 전체 조회, 값이 있으면 특정 레퍼런스 조회)
     * @param cursorId    커서 ID
     * @param pageable    페이징 정보
     * @return UserLink 목록
     */
    List<UserLink> findUserLinksWithCursor(Long userId, Long referenceId, Long cursorId, Pageable pageable);

    /**
     * 사용자의 status별 링크 개수 집계
     * 
     * @param userId 사용자 ID
     * @return StatusCountProjection 리스트
     */
    List<StatusCountProjection> countByUserIdGroupByStatus(Long userId);

    /**
     * 사용자의 요일별 링크 개수 집계 (기간 제한)
     * 
     * @param userId    사용자 ID
     * @param startDate 시작 날짜
     * @return DayCountProjection 리스트
     */
    List<DayCountProjection> countByUserIdGroupByDayOfWeek(Long userId, LocalDateTime startDate);

    /**
     * 사용자의 전체 UserLink 개수 조회
     * 
     * @param userId 사용자 ID
     * @return 전체 개수
     */
    long countByUserId(Long userId);

    /**
     * 사용자의 특정 status 링크 개수 조회
     * 
     * @param userId 사용자 ID
     * @param status 링크 상태
     * @return 개수
     */
    long countByUserIdAndStatus(Long userId, LinkStatus status);


    /**
     * 공개된 UserLink 목록 조회 (Reference.isPublic = true)
     * 
     * @param pageable 페이징 정보
     * @return 공개된 UserLink 목록 (ID 내림차순)
     */
    List<UserLink> findPublicUserLinksOrderByIdDesc(Pageable pageable);

    /**
     * 공개된 모든 UserLink 목록 조회 (Reference.isPublic = true)
     * 
     * @return 공개된 전체 UserLink 목록
     */
    List<UserLink> findAllPublicUserLinks();

    /**
     * 사용자가 처음 링크를 저장한 날짜 조회
     * 
     * @param userId 사용자 ID
     * @return 첫 저장 날짜 (없으면 null)
     */
    LocalDateTime findFirstCreatedDateByUserId(Long userId);

}
