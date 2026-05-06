package swyp12.team9.server.domain.reference.service;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.google.cloud.storage.Storage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import swyp12.team9.server.ServerApplication;
import swyp12.team9.server.domain.chatbot.service.ChatbotIndexingService;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.link.model.LinkProcessingStatus;
import swyp12.team9.server.domain.link.repository.LinkRepository;
import swyp12.team9.server.domain.recommendation.service.RecommendationCacheService;
import swyp12.team9.server.domain.reference.dto.request.ReferenceUpdateRequest;
import swyp12.team9.server.domain.reference.model.Reference;
import swyp12.team9.server.domain.reference.relation.model.ReferenceUserLink;
import swyp12.team9.server.domain.reference.relation.repository.ReferenceUserLinkRepository;
import swyp12.team9.server.domain.reference.repository.ReferenceRepository;
import swyp12.team9.server.domain.user.model.User;
import swyp12.team9.server.domain.user.model.UserRole;
import swyp12.team9.server.domain.user.model.UserStatus;
import swyp12.team9.server.domain.user.repository.UserRepository;
import swyp12.team9.server.domain.userlink.model.UserLink;
import swyp12.team9.server.domain.userlink.repository.UserLinkRepository;

@SpringBootTest(classes = ServerApplication.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ActiveProfiles("test")
@DirtiesContext
@DisplayName("Reference 공개 상태 변경 통합 테스트")
class ReferenceVisibilityIntegrationTest {

    @Autowired
    private ReferenceService referenceService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReferenceRepository referenceRepository;

    @Autowired
    private LinkRepository linkRepository;

    @Autowired
    private UserLinkRepository userLinkRepository;

    @Autowired
    private ReferenceUserLinkRepository referenceUserLinkRepository;

    @MockBean
    private VectorStore vectorStore;

    @MockBean
    private RecommendationCacheService recommendationCacheService;

    @MockBean
    private ChatbotIndexingService chatbotIndexingService;

    @MockBean
    private Storage storage;

    @Test
    @DisplayName("성공: 비공개 폴더를 공개로 바꾸면 커밋 후 추천 인덱싱과 캐시 무효화가 실행된다")
    void success_reindexAfterReferenceBecomesPublic() {
        // given
        User user = userRepository.save(createUser());
        Reference reference = referenceRepository.save(Reference.create(user, "ref", null, false, "#000000"));
        Link link = linkRepository.save(createLink("https://example.com/public"));
        UserLink userLink = userLinkRepository.save(UserLink.builder()
                .user(user)
                .link(link)
                .why("why")
                .memo("memo")
                .build());
        referenceUserLinkRepository.save(ReferenceUserLink.create(reference, userLink));

        // when
        referenceService.updateReference(
                user.getId(),
                reference.getId(),
                new ReferenceUpdateRequest(null, null, true, null)
        );

        // then
        verify(vectorStore, timeout(3000)).add(anyList());
        verify(recommendationCacheService, timeout(3000)).evictCategoryRecommendationIds();
    }

    @Test
    @DisplayName("성공: 공개 폴더를 비공개로 바꾸면 커밋 후 추천 인덱스 삭제와 캐시 무효화가 실행된다")
    void success_deleteIndexAfterReferenceBecomesPrivate() {
        // given
        User user = userRepository.save(createUser());
        Reference reference = referenceRepository.save(Reference.create(user, "ref2", null, true, "#000000"));
        Link link = linkRepository.save(createLink("https://example.com/private"));
        UserLink userLink = userLinkRepository.save(UserLink.builder()
                .user(user)
                .link(link)
                .why("why")
                .memo("memo")
                .build());
        referenceUserLinkRepository.save(ReferenceUserLink.create(reference, userLink));

        // when
        referenceService.updateReference(
                user.getId(),
                reference.getId(),
                new ReferenceUpdateRequest(null, null, false, null)
        );

        // then
        verify(vectorStore, timeout(3000)).delete(java.util.List.of("recommendation-" + userLink.getId()));
        verify(recommendationCacheService, timeout(3000)).evictCategoryRecommendationIds();
    }

    private User createUser() {
        return User.builder()
                .username("user-" + java.util.UUID.randomUUID())
                .password("password")
                .isLock(false)
                .isSocial(false)
                .roleType(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
    }

    private Link createLink(String url) {
        return Link.builder()
                .url(url)
                .title("title")
                .description("description")
                .faviconUrl("https://example.com/favicon.ico")
                .content("content")
                .aiSummary("summary")
                .processingStatus(LinkProcessingStatus.READY)
                .build();
    }
}
