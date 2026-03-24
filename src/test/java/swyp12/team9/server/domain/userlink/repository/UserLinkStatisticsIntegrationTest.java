package swyp12.team9.server.domain.userlink.repository;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import swyp12.team9.server.domain.link.fixture.LinkFixture;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.link.repository.LinkRepository;
import swyp12.team9.server.domain.reference.fixture.ReferenceFixture;
import swyp12.team9.server.domain.reference.model.Reference;
import swyp12.team9.server.domain.reference.relation.dto.ReferenceCategoryCountProjection;
import swyp12.team9.server.domain.reference.relation.model.ReferenceUserLink;
import swyp12.team9.server.domain.reference.relation.repository.ReferenceUserLinkRepository;
import swyp12.team9.server.domain.reference.repository.ReferenceRepository;
import swyp12.team9.server.domain.user.fixture.UserFixture;
import swyp12.team9.server.domain.user.model.User;
import swyp12.team9.server.domain.user.repository.UserRepository;
import swyp12.team9.server.domain.userlink.dto.DayCountProjection;
import swyp12.team9.server.domain.userlink.fixture.UserLinkFixture;
import swyp12.team9.server.domain.userlink.model.LinkStatus;
import swyp12.team9.server.domain.userlink.model.UserLink;
import swyp12.team9.server.support.IntegrationTestSupport;

@DisplayName("UserLink 통계 집계 통합 테스트")
class UserLinkStatisticsIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LinkRepository linkRepository;

    @Autowired
    private UserLinkRepository userLinkRepository;

    @Autowired
    private ReferenceRepository referenceRepository;

    @Autowired
    private ReferenceUserLinkRepository referenceUserLinkRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("성공: 마이페이지 통계 집계에서는 READY 링크만 반영한다")
    void success_UserStatAggregationsIncludeOnlyReadyLinks() {
        User user = userRepository.save(UserFixture.createUser());
        Reference reference = referenceRepository.save(ReferenceFixture.createPrivateReference(user));

        UserLink readyUserLink = saveUserLink(user, reference, createReadyLink("https://example.com/stat-ready"));
        saveUserLink(user, reference, createFailedLink("https://example.com/stat-failed"));
        saveUserLink(user, reference, createPendingLink("https://example.com/stat-pending"));
        readyUserLink.markAsRead();

        flushAndClear();

        assertThat(userLinkRepository.countByUserId(user.getId())).isEqualTo(1L);
        assertThat(userLinkRepository.countByUserIdAndStatus(user.getId(), LinkStatus.READ)).isEqualTo(1L);
        assertThat(userLinkRepository.countByUserIdAndStatus(user.getId(), LinkStatus.UNREAD)).isEqualTo(0L);
        assertThat(userLinkRepository.findFirstCreatedDateByUserId(user.getId())).isNotNull();

        List<DayCountProjection> dayCounts = userLinkRepository.countByUserIdGroupByDayOfWeek(
                user.getId(),
                readyUserLink.getCreatedAt().minusDays(1)
        );

        assertThat(dayCounts.stream().mapToLong(DayCountProjection::getCount).sum()).isEqualTo(1L);
    }

    @Test
    @DisplayName("성공: 레퍼런스 통계 집계에서는 READY 링크만 반영한다")
    void success_ReferenceStatAggregationsIncludeOnlyReadyLinks() {
        User user = userRepository.save(UserFixture.createUser());
        Reference readyReference = referenceRepository.save(ReferenceFixture.createPrivateReference(user));
        Reference failedOnlyReference = referenceRepository.save(
                ReferenceFixture.createReferenceWithTitleAndPublic(user, "failed-only", false)
        );
        Reference pendingOnlyReference = referenceRepository.save(
                ReferenceFixture.createReferenceWithTitleAndPublic(user, "pending-only", false)
        );

        UserLink readyUserLink = saveUserLink(user, readyReference, createReadyLink("https://example.com/top-ready"));
        readyUserLink.markAsRead();
        saveUserLink(user, failedOnlyReference, createFailedLink("https://example.com/top-failed"));
        saveUserLink(user, pendingOnlyReference, createPendingLink("https://example.com/top-pending"));

        flushAndClear();

        List<ReferenceCategoryCountProjection> grouped = referenceUserLinkRepository.countByUserIdGroupByReference(user.getId());
        List<ReferenceCategoryCountProjection> unreadGrouped = referenceUserLinkRepository.countUnreadByUserIdGroupByReference(user.getId());

        var countByReferenceId = grouped.stream()
                .collect(toMap(ReferenceCategoryCountProjection::getReferenceId, ReferenceCategoryCountProjection::getCount));

        assertThat(countByReferenceId.get(readyReference.getId())).isEqualTo(1L);
        assertThat(countByReferenceId.get(failedOnlyReference.getId())).isZero();
        assertThat(countByReferenceId.get(pendingOnlyReference.getId())).isZero();
        assertThat(unreadGrouped).isEmpty();
    }

    private UserLink saveUserLink(User user, Reference reference, Link link) {
        Link savedLink = linkRepository.save(link);
        UserLink savedUserLink = userLinkRepository.save(UserLinkFixture.createUserLink(user, savedLink));
        referenceUserLinkRepository.save(ReferenceUserLink.create(reference, savedUserLink));
        return savedUserLink;
    }

    private Link createReadyLink(String url) {
        return LinkFixture.createLinkWithUrl(url);
    }

    private Link createFailedLink(String url) {
        Link link = Link.createPlaceholder(url);
        link.markFailed();
        return link;
    }

    private Link createPendingLink(String url) {
        return Link.createPlaceholder(url);
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
