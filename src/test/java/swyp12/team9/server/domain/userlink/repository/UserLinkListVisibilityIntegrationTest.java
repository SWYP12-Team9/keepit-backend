package swyp12.team9.server.domain.userlink.repository;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import swyp12.team9.server.domain.link.fixture.LinkFixture;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.link.repository.LinkRepository;
import swyp12.team9.server.domain.reference.dto.ReferenceSortType;
import swyp12.team9.server.domain.reference.dto.ReferenceType;
import swyp12.team9.server.domain.reference.dto.response.ReferenceListResponse;
import swyp12.team9.server.domain.reference.fixture.ReferenceFixture;
import swyp12.team9.server.domain.reference.model.Reference;
import swyp12.team9.server.domain.reference.relation.model.ReferenceUserLink;
import swyp12.team9.server.domain.reference.relation.repository.ReferenceUserLinkRepository;
import swyp12.team9.server.domain.reference.repository.ReferenceRepository;
import swyp12.team9.server.domain.user.fixture.UserFixture;
import swyp12.team9.server.domain.user.model.User;
import swyp12.team9.server.domain.user.repository.UserRepository;
import swyp12.team9.server.domain.userlink.fixture.UserLinkFixture;
import swyp12.team9.server.domain.userlink.model.UserLink;
import swyp12.team9.server.support.IntegrationTestSupport;

@DisplayName("UserLink 목록 조회 visibility 통합 테스트")
class UserLinkListVisibilityIntegrationTest extends IntegrationTestSupport {

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
    @DisplayName("성공: 레퍼런스 내부 목록 조회에서는 FAILED가 아닌 링크를 노출한다")
    void success_FindUserLinksWithCursorByReferenceExcludesOnlyFailed() {
        User user = userRepository.save(UserFixture.createUser());
        Reference reference = referenceRepository.save(ReferenceFixture.createPrivateReference(user));

        UserLink readyUserLink = saveUserLink(user, reference, createReadyLink("https://example.com/ready"));
        UserLink pendingUserLink = saveUserLink(user, reference, createPendingLink("https://example.com/pending"));
        saveUserLink(user, reference, createFailedLink("https://example.com/failed"));

        flushAndClear();

        List<UserLink> results = userLinkRepository.findUserLinksWithCursor(
                user.getId(),
                reference.getId(),
                null,
                PageRequest.of(0, 10)
        );

        assertThat(results)
                .extracting(UserLink::getId)
                .containsExactly(pendingUserLink.getId(), readyUserLink.getId());
    }

    @Test
    @DisplayName("성공: 전체 내 링크 목록 조회에서는 FAILED가 아닌 링크를 노출한다")
    void success_FindUserLinksWithCursorAllExcludesOnlyFailed() {
        User user = userRepository.save(UserFixture.createUser());
        Reference reference = referenceRepository.save(ReferenceFixture.createPrivateReference(user));

        UserLink readyUserLink = saveUserLink(user, reference, createReadyLink("https://example.com/all-ready"));
        UserLink pendingUserLink = saveUserLink(user, reference, createPendingLink("https://example.com/all-pending"));
        saveUserLink(user, reference, createFailedLink("https://example.com/all-failed"));

        flushAndClear();

        List<UserLink> results = userLinkRepository.findUserLinksWithCursor(
                user.getId(),
                null,
                null,
                PageRequest.of(0, 10)
        );

        assertThat(results)
                .extracting(UserLink::getId)
                .containsExactly(pendingUserLink.getId(), readyUserLink.getId());
    }

    @Test
    @DisplayName("성공: 공개 링크 목록 조회에서는 READY 링크만 노출한다")
    void success_PublicListQueriesExcludeNonReadyLinks() {
        User user = userRepository.save(UserFixture.createUser());
        Reference publicReference = referenceRepository.save(ReferenceFixture.createPublicReference(user));

        UserLink readyUserLink = saveUserLink(user, publicReference, createReadyLink("https://example.com/public-ready"));
        saveUserLink(user, publicReference, createFailedLink("https://example.com/public-failed"));
        saveUserLink(user, publicReference, createPendingLink("https://example.com/public-pending"));

        flushAndClear();

        List<UserLink> pagedResults = userLinkRepository.findPublicUserLinksOrderByIdDesc(PageRequest.of(0, 10));
        List<UserLink> allResults = userLinkRepository.findAllByReferenceIsPublicTrue();

        assertThat(pagedResults)
                .extracting(UserLink::getId)
                .containsExactly(readyUserLink.getId());
        assertThat(allResults)
                .extracting(UserLink::getId)
                .containsExactly(readyUserLink.getId());
    }

    @Test
    @DisplayName("성공: 레퍼런스 링크 개수는 READY 링크만 계산한다")
    void success_FindAllWithLinkCountCountsOnlyReadyLinks() {
        User user = userRepository.save(UserFixture.createUser());
        Reference reference = referenceRepository.save(ReferenceFixture.createPrivateReference(user));

        saveUserLink(user, reference, createReadyLink("https://example.com/ref-ready"));
        saveUserLink(user, reference, createFailedLink("https://example.com/ref-failed"));
        saveUserLink(user, reference, createPendingLink("https://example.com/ref-pending"));

        flushAndClear();

        List<ReferenceListResponse> results = referenceRepository.findAllWithLinkCount(
                user.getId(),
                ReferenceType.ALL,
                ReferenceSortType.CREATED_DESC,
                null,
                10
        );

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().linkCount()).isEqualTo(1L);
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
