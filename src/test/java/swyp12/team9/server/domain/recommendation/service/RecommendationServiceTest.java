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
import swyp12.team9.server.domain.userlink.dto.PopularLinkProjection;
import swyp12.team9.server.domain.userlink.model.UserLink;
import swyp12.team9.server.domain.userlink.model.LinkStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

    @Test
    @DisplayName("인기글 목록 조회 시 페이징 처리 및 DTO 변환이 정상적으로 수행된다")
    void getPopularPublicLinks_Success() {
        // given
        Long userId = 1L; // 요청 유저
        String cursor = null; // 첫 페이지 요청
        int size = 1;

        // 깔끔해진 준비 과정 (도우미 메서드 활용)
        Link mockLink = createMockLink(100L);
        User mockUser = createMockUser(10L, "테스터");
        UserLink mockUserLink = createMockUserLink(50L, mockUser, mockLink);

        PopularLinkProjection projection = new PopularLinkProjection(mockLink.getId(), 500L);
        
        given(userLinkRepository.findPopularPublicLinks(isNull(), isNull(), eq(size)))
                .willReturn(List.of(projection));
        given(userLinkRepository.findFirstPublicUserLinksByLinkIds(anyList()))
                .willReturn(List.of(mockUserLink));

        // when
        PaginationUtils.Cursor.PageResponse<RecommendationResponse> result =
                recommendationService.getPopularPublicLinks(userId, cursor, size);

        // then
        assertThat(result.isHasNext()).isFalse(); 
        assertThat(result.getNextCursor()).isNull();
        assertThat(result.getContents()).hasSize(1);

        RecommendationResponse response = result.getContents().get(0);
        
        assertThat(response.id()).isNotNull();
        assertThat(response.publicViewCount()).isGreaterThanOrEqualTo(0L);
        assertThat(response.user().nickname()).isEqualTo("테스터");

        checkSerializationCompatibility(response);
    }

    /**
     * [승인 테스트] JSON 직렬화/역직렬화가 깨지지 않는지 확인합니다.
     * 필드명을 바꾸거나 record 구조를 변경했을 때 Redis에서 터지는 걸 미리 방지합니다.
     */
    private void checkSerializationCompatibility(RecommendationResponse original) {
        try {
            ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
            
            // 1. 객체 -> JSON (직렬화)
            String json = mapper.writeValueAsString(original);
            
            // 2. JSON -> 객체 (역직렬화)
            RecommendationResponse recovered = mapper.readValue(json, RecommendationResponse.class);
            
            // 3. 복원된 데이터가 원본과 일치하는가?
            assertThat(recovered.id()).isEqualTo(original.id());
            assertThat(recovered.title()).isEqualTo(original.title());
            assertThat(recovered.user().nickname()).isEqualTo(original.user().nickname());
            
        } catch (Exception e) {
            throw new AssertionError("포장 용기(직렬화) 최적화 실패! Redis에서 에러 날 확률 100%입니다: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("인기글 목록이 없을 경우 빈 페이지를 반환한다")
    void getPopularPublicLinks_Empty() {
        // given
        Long userId = 1L;
        String cursor = null;
        int size = 20;

        given(userLinkRepository.findPopularPublicLinks(isNull(), isNull(), eq(size)))
                .willReturn(List.of());

        // when
        PaginationUtils.Cursor.PageResponse<RecommendationResponse> result =
                recommendationService.getPopularPublicLinks(userId, cursor, size);

        // then
        assertThat(result.getContents()).isEmpty();
        assertThat(result.isHasNext()).isFalse();
        assertThat(result.getNextCursor()).isNull();
    }

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
