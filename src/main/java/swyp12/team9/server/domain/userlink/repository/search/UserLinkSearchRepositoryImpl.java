package swyp12.team9.server.domain.userlink.repository.search;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import swyp12.team9.server.domain.userlink.model.UserLink;

import java.util.List;

import static swyp12.team9.server.domain.link.model.QLink.link;
import static swyp12.team9.server.domain.reference.model.QReference.reference;
import static swyp12.team9.server.domain.referenceuserlink.model.QReferenceUserLink.referenceUserLink;
import static swyp12.team9.server.domain.userlink.model.QUserLink.userLink;

/**
 * UserLink 검색 Repository QueryDSL 구현체
 * <p>
 * 모든 검색 쿼리를 QueryDSL로 통일하여 타입 안전성과 유지보수성을 높입니다.
 * <p>
 * 주요 특징:
 * - 동적 필드 검색: field 파라미터에 따라 검색 대상 필드가 결정됨
 * - 커서 기반 페이징: ID 기반 무한 스크롤 지원
 * - N:N 관계 처리: ReferenceUserLink를 통한 조인
 */
@RequiredArgsConstructor
public class UserLinkSearchRepositoryImpl implements UserLinkSearchRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    // ==================== 검색 가능 필드 상수 ====================
    private static final String FIELD_WHY = "why";
    private static final String FIELD_MEMO = "memo";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_AI_SUMMARY = "aiSummary";
    private static final String FIELD_URL = "url";

    // ==================== 내 링크 검색 ====================

    /**
     * 내 링크에서 키워드 검색 (커서 기반)
     * <p>
     * 검색 대상: UserLink.why, UserLink.memo, Link.title, Link.aiSummary, Link.url
     */
    @Override
    public List<UserLink> searchMyLinks(Long userId, String keyword, String field, Long cursorId, int size) {
        return queryFactory
                .selectFrom(userLink)
                .join(userLink.link, link).fetchJoin()  // N+1 방지를 위한 fetch join
                .where(
                        userLink.user.id.eq(userId),
                        cursorCondition(cursorId),
                        keywordCondition(keyword, field)
                )
                .orderBy(userLink.id.desc())
                .limit(size)
                .fetch();
    }

    // ==================== 레퍼런스 폴더 내 검색 ====================

    /**
     * 특정 레퍼런스 폴더 내 링크 검색 (커서 기반)
     * <p>
     * - ReferenceUserLink를 통해 레퍼런스와 연결된 모든 UserLink 검색
     * - 내 링크뿐 아니라 다른 사용자의 링크도 포함
     */
    @Override
    public List<UserLink> searchByReference(Long referenceId, String keyword, String field, Long cursorId, int size) {
        return queryFactory
                .selectFrom(userLink)
                .join(userLink.link, link).fetchJoin()
                .join(referenceUserLink).on(referenceUserLink.userLink.eq(userLink))
                .where(
                        referenceUserLink.reference.id.eq(referenceId),
                        cursorCondition(cursorId),
                        keywordCondition(keyword, field)
                )
                .orderBy(userLink.id.desc())
                .limit(size)
                .fetch();
    }

    // ==================== 내 모든 레퍼런스 폴더 내 검색 ====================

    /**
     * 내 모든 레퍼런스 폴더 내 링크 검색 (커서 기반)
     * <p>
     * - 내가 소유한 모든 레퍼런스 폴더에 속한 UserLink 검색
     * - DISTINCT로 중복 제거 (하나의 링크가 여러 폴더에 있을 수 있음)
     */
    @Override
    public List<UserLink> searchAllMyReferences(Long userId, String keyword, String field, Long cursorId, int size) {
        return queryFactory
                .selectFrom(userLink)
                .distinct()
                .join(userLink.link, link).fetchJoin()
                .join(referenceUserLink).on(referenceUserLink.userLink.eq(userLink))
                .join(referenceUserLink.reference, reference)
                .where(
                        reference.user.id.eq(userId),
                        cursorCondition(cursorId),
                        keywordCondition(keyword, field)
                )
                .orderBy(userLink.id.desc())
                .limit(size)
                .fetch();
    }

    // ==================== 조건 빌더 메서드 ====================

    /**
     * 커서 조건: id < cursorId
     * <p>
     * cursorId가 null이면 첫 페이지이므로 조건 없음
     */
    private BooleanExpression cursorCondition(Long cursorId) {
        return cursorId != null ? userLink.id.lt(cursorId) : null;
    }

    /**
     * 키워드 검색 조건 (동적 필드 선택)
     * <p>
     * field가 null이면 모든 필드에서 검색
     * field가 지정되면 해당 필드에서만 검색
     *
     * @param keyword 검색 키워드
     * @param field   검색 필드 (why, memo, title, aiSummary, url)
     * @return 검색 조건 BooleanExpression
     */
    private BooleanExpression keywordCondition(String keyword, String field) {
        if (keyword == null || keyword.isEmpty()) {
            return null;
        }

        String lowerKeyword = keyword.toLowerCase();

        // 특정 필드만 검색
        if (field != null && !field.isEmpty()) {
            return fieldContains(field, lowerKeyword);
        }

        // 전체 필드 검색 (OR 조건)
        return containsIgnoreCase(userLink.why, lowerKeyword)
                .or(containsIgnoreCase(userLink.memo, lowerKeyword))
                .or(containsIgnoreCase(link.title, lowerKeyword))
                .or(containsIgnoreCase(link.aiSummary, lowerKeyword))
                .or(containsIgnoreCase(link.url, lowerKeyword));
    }

    /**
     * 특정 필드에서 키워드 검색
     * <p>
     * CASE WHEN 대신 switch 문으로 필드별 조건 생성
     */
    private BooleanExpression fieldContains(String field, String lowerKeyword) {
        return switch (field) {
            case FIELD_WHY -> containsIgnoreCase(userLink.why, lowerKeyword);
            case FIELD_MEMO -> containsIgnoreCase(userLink.memo, lowerKeyword);
            case FIELD_TITLE -> containsIgnoreCase(link.title, lowerKeyword);
            case FIELD_AI_SUMMARY -> containsIgnoreCase(link.aiSummary, lowerKeyword);
            case FIELD_URL -> containsIgnoreCase(link.url, lowerKeyword);
            default -> null;  // 알 수 없는 필드는 무시
        };
    }

    /**
     * 대소문자 무시 LIKE 검색
     * <p>
     * SQL: LOWER(field) LIKE LOWER('%keyword%')
     */
    private BooleanExpression containsIgnoreCase(StringPath path, String lowerKeyword) {
        return path.lower().contains(lowerKeyword);
    }
}
