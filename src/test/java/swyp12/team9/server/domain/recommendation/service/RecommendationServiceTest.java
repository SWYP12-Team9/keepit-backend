package swyp12.team9.server.domain.recommendation.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.link.model.LinkProcessingStatus;
import swyp12.team9.server.domain.recommendation.dto.RecommendationResponse;
import swyp12.team9.server.domain.user.model.SocialProvider;
import swyp12.team9.server.domain.user.model.User;
import swyp12.team9.server.domain.userlink.model.UserLink;
import swyp12.team9.server.domain.userlink.model.LinkStatus;
import swyp12.team9.server.domain.userlink.repository.UserLinkRepository;
import swyp12.team9.server.global.util.PaginationUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @InjectMocks
    private RecommendationService recommendationService;

    @Mock
    private UserLinkRepository userLinkRepository;


    // ==========================================
    // 도우미 메서드 (Fixture Helpers)
    // ==========================================
    
    private Link createMockLink(Long id) {
        Link link = Link.builder()
                .url("https://example.com/java")
                .title("자바 기초")
                .faviconUrl("https://example.com/favicon.png")
                .aiSummary("자바 기본 문법")
                .processingStatus(LinkProcessingStatus.READY)
                .build();
        setField(link, "id", id);
        return link;
    }

    private User createMockUser(Long id, String nickname) {
        User user = User.builder()
                .nickname(nickname)
                .profileImageUrl("https://img.com")
                .build();
        setField(user, "id", id);
        return user;
    }

    private UserLink createMockUserLink(Long id, User user, Link link) {
        UserLink userLink = UserLink.builder()
                .user(user)
                .link(link)
                .build();
        setField(userLink, "id", id);
        return userLink;
    }
}
