package swyp12.team9.server.domain.recommendation.service;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.test.util.ReflectionTestUtils;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.link.model.LinkProcessingStatus;
import swyp12.team9.server.domain.recommendation.service.RecommendationCacheService;
import swyp12.team9.server.domain.user.model.User;
import swyp12.team9.server.domain.user.model.UserRole;
import swyp12.team9.server.domain.user.model.UserStatus;
import swyp12.team9.server.domain.userlink.model.UserLink;
import swyp12.team9.server.domain.userlink.repository.UserLinkRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("LinkIndexingService 단위 테스트")
class LinkIndexingServiceTest {

    @Mock
    private UserLinkRepository userLinkRepository;

    @Mock
    private VectorStore vectorStore;

    @Mock
    private RecommendationCacheService recommendationCacheService;

    @InjectMocks
    private LinkIndexingService linkIndexingService;

    @Nested
    @DisplayName("indexUserLink 테스트")
    class IndexUserLink {

        @Test
        @DisplayName("성공: 추천 인덱싱 성공 후 캐시를 무효화한다")
        void success_evictCacheAfterSuccessfulIndexing() {
            // given
            Long userLinkId = 1L;
            UserLink userLink = createValidUserLink(userLinkId);

            given(userLinkRepository.findById(userLinkId)).willReturn(Optional.of(userLink));
            given(userLinkRepository.isUserLinkInPublicReference(userLinkId)).willReturn(true);

            // when
            linkIndexingService.indexUserLink(userLinkId);

            // then
            InOrder inOrder = inOrder(vectorStore, recommendationCacheService);
            inOrder.verify(vectorStore).add(anyList());
            inOrder.verify(recommendationCacheService).evictCategoryRecommendationIds();
        }

        @Test
        @DisplayName("성공: 추천 대상이 아니면 인덱스 삭제 후 캐시를 무효화한다")
        void success_evictCacheAfterDeletingIndex() {
            // given
            Long userLinkId = 2L;
            UserLink userLink = createValidUserLink(userLinkId);

            given(userLinkRepository.findById(userLinkId)).willReturn(Optional.of(userLink));
            given(userLinkRepository.isUserLinkInPublicReference(userLinkId)).willReturn(false);

            // when
            linkIndexingService.indexUserLink(userLinkId);

            // then
            InOrder inOrder = inOrder(vectorStore, recommendationCacheService);
            inOrder.verify(vectorStore).delete(java.util.List.of("recommendation-" + userLinkId));
            inOrder.verify(recommendationCacheService).evictCategoryRecommendationIds();
        }
    }

    private UserLink createValidUserLink(Long userLinkId) {
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
                .description("description")
                .faviconUrl("https://example.com/favicon.ico")
                .content("content")
                .aiSummary("summary")
                .processingStatus(LinkProcessingStatus.READY)
                .build();
        ReflectionTestUtils.setField(link, "id", 11L);

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
