package swyp12.team9.server.global.event;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import swyp12.team9.server.domain.reference.event.ReferenceVisibilityChangedEvent;
import swyp12.team9.server.domain.reference.model.Reference;
import swyp12.team9.server.domain.reference.relation.model.ReferenceUserLink;
import swyp12.team9.server.domain.reference.relation.repository.ReferenceUserLinkRepository;
import swyp12.team9.server.domain.recommendation.event.RecommendationIndexingEventListener;
import swyp12.team9.server.domain.recommendation.service.RecommendationCacheService;
import swyp12.team9.server.domain.recommendation.service.LinkIndexingService;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.user.model.User;
import swyp12.team9.server.domain.user.model.UserRole;
import swyp12.team9.server.domain.user.model.UserStatus;
import swyp12.team9.server.domain.userlink.model.UserLink;

@ExtendWith(MockitoExtension.class)
@DisplayName("IndexingEventListener 단위 테스트")
class IndexingEventListenerTest {

    @Mock
    private RecommendationCacheService recommendationCacheService;

    @Mock
    private LinkIndexingService linkIndexingService;

    @Mock
    private ReferenceUserLinkRepository referenceUserLinkRepository;

    @InjectMocks
    private RecommendationIndexingEventListener indexingEventListener;

    @Test
    @DisplayName("성공: Reference 공개 상태가 바뀌면 연결된 모든 UserLink를 추천 재인덱싱한다")
    void success_reindexAllUserLinksWhenReferenceVisibilityChanged() {
        // given
        Long referenceId = 20L;
        UserLink firstUserLink = createUserLink(101L);
        UserLink secondUserLink = createUserLink(102L);

        given(referenceUserLinkRepository.findByReferenceId(referenceId)).willReturn(List.of(
                ReferenceUserLink.create(createReference(referenceId), firstUserLink),
                ReferenceUserLink.create(createReference(referenceId), secondUserLink)
        ));

        // when
        indexingEventListener.handleReferenceVisibilityChanged(
                ReferenceVisibilityChangedEvent.of(referenceId, true)
        );

        // then
        verify(linkIndexingService).indexUserLink(101L);
        verify(linkIndexingService).indexUserLink(102L);
    }

    private Reference createReference(Long referenceId) {
        User user = User.builder()
                .username("tester")
                .password("password")
                .isLock(false)
                .isSocial(false)
                .roleType(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        Reference reference = Reference.create(user, "ref", null, true, "#000000");
        ReflectionTestUtils.setField(reference, "id", referenceId);
        return reference;
    }

    private UserLink createUserLink(Long userLinkId) {
        User user = User.builder()
                .username("tester-" + userLinkId)
                .password("password")
                .isLock(false)
                .isSocial(false)
                .roleType(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        Link link = Link.builder()
                .url("https://example.com/" + userLinkId)
                .title("title")
                .description("desc")
                .faviconUrl("https://example.com/favicon.ico")
                .content("content")
                .aiSummary("summary")
                .build();

        UserLink userLink = UserLink.builder()
                .user(user)
                .link(link)
                .why("why")
                .memo("memo")
                .build();
        ReflectionTestUtils.setField(userLink, "id", userLinkId);
        return userLink;
    }
}
