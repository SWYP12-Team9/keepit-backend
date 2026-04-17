package swyp12.team9.server.domain.popular.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.link.model.LinkProcessingStatus;
import swyp12.team9.server.domain.popular.dto.PopularLinkProjection;
import swyp12.team9.server.domain.popular.dto.PopularResponse;
import swyp12.team9.server.domain.popular.repository.PopularRepository;
import swyp12.team9.server.domain.user.model.User;
import swyp12.team9.server.domain.userlink.model.UserLink;
import swyp12.team9.server.global.util.PaginationUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
class PopularServiceTest {

    @InjectMocks
    private PopularService popularService;

    @Mock
    private PopularRepository popularRepository;

    @Test
    @DisplayName("인기글 목록 조회 시 페이징 처리 및 DTO 변환이 정상적으로 수행된다")
    void getPopularPublicLinks_Success() {
        // given
        Long userId = 1L;
        String cursor = null;
        int size = 1;

        Link mockLink = createMockLink(100L);
        User mockUser = createMockUser(10L, "테스터");
        UserLink mockUserLink = createMockUserLink(50L, mockUser, mockLink);

        PopularLinkProjection projection = new PopularLinkProjection(mockLink.getId(), 500L);

        given(popularRepository.findPopularPublicLinks(isNull(), isNull(), eq(size)))
                .willReturn(List.of(projection));
        given(popularRepository.findFirstPublicUserLinksByLinkIds(anyList()))
                .willReturn(List.of(mockUserLink));

        // when
        PaginationUtils.Cursor.PageResponse<PopularResponse> result =
                popularService.getPopularPublicLinks(userId, cursor, size);

        // then
        assertThat(result.isHasNext()).isFalse();
        assertThat(result.getNextCursor()).isNull();
        assertThat(result.getContents()).hasSize(1);

        PopularResponse response = result.getContents().get(0);

        assertThat(response.id()).isNotNull();
        assertThat(response.publicViewCount()).isGreaterThanOrEqualTo(0L);
        assertThat(response.user().nickname()).isEqualTo("테스터");

        checkSerializationCompatibility(response);
    }

    private void checkSerializationCompatibility(PopularResponse original) {
        try {
            ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
            String json = mapper.writeValueAsString(original);
            PopularResponse recovered = mapper.readValue(json, PopularResponse.class);

            assertThat(recovered.id()).isEqualTo(original.id());
            assertThat(recovered.title()).isEqualTo(original.title());
            assertThat(recovered.user().nickname()).isEqualTo(original.user().nickname());

        } catch (Exception e) {
            throw new AssertionError("포장 용기(직렬화) 최적화 실패!: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("인기글 목록이 없을 경우 빈 페이지를 반환한다")
    void getPopularPublicLinks_Empty() {
        // given
        Long userId = 1L;
        String cursor = null;
        int size = 20;

        given(popularRepository.findPopularPublicLinks(isNull(), isNull(), eq(size)))
                .willReturn(List.of());

        // when
        PaginationUtils.Cursor.PageResponse<PopularResponse> result =
                popularService.getPopularPublicLinks(userId, cursor, size);

        // then
        assertThat(result.getContents()).isEmpty();
        assertThat(result.isHasNext()).isFalse();
        assertThat(result.getNextCursor()).isNull();
    }

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
