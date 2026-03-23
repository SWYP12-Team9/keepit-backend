package swyp12.team9.server.domain.userlink.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.link.repository.LinkRepository;
import swyp12.team9.server.domain.reference.fixture.ReferenceFixture;
import swyp12.team9.server.domain.reference.model.Reference;
import swyp12.team9.server.domain.reference.relation.model.ReferenceUserLink;
import swyp12.team9.server.domain.reference.relation.repository.ReferenceUserLinkRepository;
import swyp12.team9.server.domain.reference.repository.ReferenceRepository;
import swyp12.team9.server.domain.user.fixture.UserFixture;
import swyp12.team9.server.domain.user.model.User;
import swyp12.team9.server.domain.user.repository.UserRepository;
import swyp12.team9.server.domain.userlink.exception.UserLinkNotFoundException;
import swyp12.team9.server.domain.userlink.fixture.UserLinkFixture;
import swyp12.team9.server.domain.userlink.model.UserLink;
import swyp12.team9.server.domain.userlink.repository.UserLinkRepository;
import swyp12.team9.server.support.IntegrationTestSupport;

@DisplayName("UserLink 노출 visibility 통합 테스트")
class UserLinkVisibilityIntegrationTest extends IntegrationTestSupport {

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
    private UserLinkService userLinkService;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("실패: 단건 조회에서 PENDING 링크를 조회하면 UserLinkNotFoundException이 발생한다")
    void fail_GetUserLinkWhenPendingLink() {
        User user = userRepository.save(UserFixture.createUser());
        Reference reference = referenceRepository.save(ReferenceFixture.createPrivateReference(user));
        UserLink pendingUserLink = saveUserLink(user, reference, Link.createPlaceholder("https://example.com/pending-detail"));

        flushAndClear();

        assertThatThrownBy(() -> userLinkService.getUserLink(user.getId(), pendingUserLink.getId()))
                .isInstanceOf(UserLinkNotFoundException.class);
    }

    @Test
    @DisplayName("실패: preview 조회에서 FAILED 링크를 조회하면 UserLinkNotFoundException이 발생한다")
    void fail_GetUserLinkPreviewWhenFailedLink() {
        User user = userRepository.save(UserFixture.createUser());
        Reference reference = referenceRepository.save(ReferenceFixture.createPrivateReference(user));
        Link failedLink = Link.createPlaceholder("https://example.com/failed-card");
        failedLink.markFailed();
        UserLink failedUserLink = saveUserLink(user, reference, failedLink);

        flushAndClear();

        assertThatThrownBy(() -> userLinkService.getUserLinkPreview(user.getId(), failedUserLink.getId()))
                .isInstanceOf(UserLinkNotFoundException.class);
    }

    private UserLink saveUserLink(User user, Reference reference, Link link) {
        Link savedLink = linkRepository.save(link);
        UserLink savedUserLink = userLinkRepository.save(UserLinkFixture.createUserLink(user, savedLink));
        referenceUserLinkRepository.save(ReferenceUserLink.create(reference, savedUserLink));
        return savedUserLink;
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
