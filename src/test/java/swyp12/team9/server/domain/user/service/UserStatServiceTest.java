package swyp12.team9.server.domain.user.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import swyp12.team9.server.api.stat.dto.UserStatResponse;
import swyp12.team9.server.domain.referenceuserlink.dto.ReferenceCategoryCountProjection;
import swyp12.team9.server.domain.referenceuserlink.repository.ReferenceUserLinkRepository;
import swyp12.team9.server.domain.stat.service.UserStatService;
import swyp12.team9.server.domain.userlink.dto.DayCountProjection;
import swyp12.team9.server.domain.userlink.model.LinkStatus;
import swyp12.team9.server.domain.userlink.repository.UserLinkRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
/**
 * UserStatService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserStatService 테스트")
class UserStatServiceTest {

    @Mock
    private UserLinkRepository userLinkRepository;

    @Mock
    private ReferenceUserLinkRepository referenceUserLinkRepository;

    @InjectMocks
    private UserStatService userStatService;

    private static final Long TEST_USER_ID = 1L;

    @Nested
    @DisplayName("getUserStat - 사용자 통계 조회")
    class GetUserStat {

        @Nested
        @DisplayName("인기 레퍼런스 순위 테스트")
        class TopReferencesTest {

            @Test
            @DisplayName("성공: 폴더 0개인 경우 - 아직 레퍼런스 폴더가 없어요")
            void success_NoReferences() {
                // given
                given(referenceUserLinkRepository.countByUserIdGroupByReference(TEST_USER_ID))
                        .willReturn(Collections.emptyList());
                given(userLinkRepository.countByUserIdAndStatus(TEST_USER_ID, LinkStatus.READ))
                        .willReturn(0L);
                given(userLinkRepository.countByUserIdAndStatus(TEST_USER_ID, LinkStatus.UNREAD))
                        .willReturn(0L);
                given(referenceUserLinkRepository.countUnreadByUserIdGroupByReference(TEST_USER_ID))
                        .willReturn(Collections.emptyList());
                given(userLinkRepository.findFirstCreatedDateByUserId(TEST_USER_ID))
                        .willReturn(null);

                // when
                UserStatResponse result = userStatService.getUserStat(TEST_USER_ID);

                // then
                assertThat(result).isNotNull();
                assertThat(result.topReferences()).isNotNull();
                assertThat(result.topReferences().references()).isEmpty();
                assertThat(result.topReferences().text()).isEqualTo("아직 레퍼런스 폴더가 없어요. 폴더를 만들고 링크를 모아보세요.");
            }

            @Test
            @DisplayName("성공: 1위 폴더 단독인 경우")
            void success_SingleTopReference() {
                // given
                List<ReferenceCategoryCountProjection> references = Arrays.asList(
                        new ReferenceCategoryCountProjection(1L, "개발", "#FF5733", 5L),
                        new ReferenceCategoryCountProjection(2L, "디자인", "#33FF57", 3L)
                );

                given(referenceUserLinkRepository.countByUserIdGroupByReference(TEST_USER_ID))
                        .willReturn(references);
                given(userLinkRepository.countByUserIdAndStatus(TEST_USER_ID, LinkStatus.READ))
                        .willReturn(0L);
                given(userLinkRepository.countByUserIdAndStatus(TEST_USER_ID, LinkStatus.UNREAD))
                        .willReturn(0L);
                given(referenceUserLinkRepository.countUnreadByUserIdGroupByReference(TEST_USER_ID))
                        .willReturn(Collections.emptyList());
                given(userLinkRepository.findFirstCreatedDateByUserId(TEST_USER_ID))
                        .willReturn(null);

                // when
                UserStatResponse result = userStatService.getUserStat(TEST_USER_ID);

                // then
                assertThat(result.topReferences().text()).isEqualTo("요즘 가장 많이 모아둔 폴더는 '개발'이에요. 링크가 총 5개예요.");
                assertThat(result.topReferences().references()).hasSize(2);
                assertThat(result.topReferences().references().getFirst().title()).isEqualTo("개발");
                assertThat(result.topReferences().references().getFirst().linkCount()).isEqualTo(5L);
            }

            @Test
            @DisplayName("성공: 1위 폴더 동률(2개)")
            void success_TwoTopReferences() {
                // given
                List<ReferenceCategoryCountProjection> references = Arrays.asList(
                        new ReferenceCategoryCountProjection(1L, "개발", "#FF5733", 5L),
                        new ReferenceCategoryCountProjection(2L, "디자인", "#33FF57", 5L),
                        new ReferenceCategoryCountProjection(3L, "기획", "#5733FF", 3L)
                );

                given(referenceUserLinkRepository.countByUserIdGroupByReference(TEST_USER_ID))
                        .willReturn(references);
                given(userLinkRepository.countByUserIdAndStatus(TEST_USER_ID, LinkStatus.READ))
                        .willReturn(0L);
                given(userLinkRepository.countByUserIdAndStatus(TEST_USER_ID, LinkStatus.UNREAD))
                        .willReturn(0L);
                given(referenceUserLinkRepository.countUnreadByUserIdGroupByReference(TEST_USER_ID))
                        .willReturn(Collections.emptyList());
                given(userLinkRepository.findFirstCreatedDateByUserId(TEST_USER_ID))
                        .willReturn(null);

                // when
                UserStatResponse result = userStatService.getUserStat(TEST_USER_ID);

                // then
                assertThat(result.topReferences().text()).isEqualTo("요즘 가장 많이 모아둔 폴더는 '개발'와 '디자인'예요. 링크가 각각 5개예요.");
            }

            @Test
            @DisplayName("성공: 1위 폴더 동률(3개 이상)")
            void success_MultipleTopReferences() {
                // given
                List<ReferenceCategoryCountProjection> references = Arrays.asList(
                        new ReferenceCategoryCountProjection(1L, "개발", "#FF5733", 5L),
                        new ReferenceCategoryCountProjection(2L, "디자인", "#33FF57", 5L),
                        new ReferenceCategoryCountProjection(3L, "기획", "#5733FF", 5L)
                );

                given(referenceUserLinkRepository.countByUserIdGroupByReference(TEST_USER_ID))
                        .willReturn(references);
                given(userLinkRepository.countByUserIdAndStatus(TEST_USER_ID, LinkStatus.READ))
                        .willReturn(0L);
                given(userLinkRepository.countByUserIdAndStatus(TEST_USER_ID, LinkStatus.UNREAD))
                        .willReturn(0L);
                given(referenceUserLinkRepository.countUnreadByUserIdGroupByReference(TEST_USER_ID))
                        .willReturn(Collections.emptyList());
                given(userLinkRepository.findFirstCreatedDateByUserId(TEST_USER_ID))
                        .willReturn(null);

                // when
                UserStatResponse result = userStatService.getUserStat(TEST_USER_ID);

                // then
                assertThat(result.topReferences().text()).isEqualTo("요즘 가장 많이 모아둔 폴더가 여러 개예요. 상위 폴더들이 각각 5개예요.");
            }

            @Test
            @DisplayName("성공: 폴더가 6개를 초과하면 상위 6개만 표시")
            void success_MoreThanSixReferences() {
                // given
                List<ReferenceCategoryCountProjection> references = Arrays.asList(
                        new ReferenceCategoryCountProjection(1L, "폴더1", "#FF5733", 8L),
                        new ReferenceCategoryCountProjection(2L, "폴더2", "#33FF57", 7L),
                        new ReferenceCategoryCountProjection(3L, "폴더3", "#5733FF", 6L),
                        new ReferenceCategoryCountProjection(4L, "폴더4", "#FF33A1", 5L),
                        new ReferenceCategoryCountProjection(5L, "폴더5", "#A133FF", 4L),
                        new ReferenceCategoryCountProjection(6L, "폴더6", "#33FFA1", 3L),
                        new ReferenceCategoryCountProjection(7L, "폴더7", "#FFA133", 2L),
                        new ReferenceCategoryCountProjection(8L, "폴더8", "#A1FF33", 1L)
                );

                given(referenceUserLinkRepository.countByUserIdGroupByReference(TEST_USER_ID))
                        .willReturn(references);
                given(userLinkRepository.countByUserIdAndStatus(TEST_USER_ID, LinkStatus.READ))
                        .willReturn(0L);
                given(userLinkRepository.countByUserIdAndStatus(TEST_USER_ID, LinkStatus.UNREAD))
                        .willReturn(0L);
                given(referenceUserLinkRepository.countUnreadByUserIdGroupByReference(TEST_USER_ID))
                        .willReturn(Collections.emptyList());
                given(userLinkRepository.findFirstCreatedDateByUserId(TEST_USER_ID))
                        .willReturn(null);

                // when
                UserStatResponse result = userStatService.getUserStat(TEST_USER_ID);

                // then
                assertThat(result.topReferences().references()).hasSize(6);
            }

            @Test
            @DisplayName("성공: 링크가 0개인 폴더도 포함되어 표시됨")
            void success_IncludeFoldersWithZeroLinks() {
                // given
                List<ReferenceCategoryCountProjection> references = Arrays.asList(
                        new ReferenceCategoryCountProjection(1L, "개발", "#FF5733", 5L),
                        new ReferenceCategoryCountProjection(2L, "디자인", "#33FF57", 3L),
                        new ReferenceCategoryCountProjection(3L, "빈폴더", "#5733FF", 0L)
                );

                given(referenceUserLinkRepository.countByUserIdGroupByReference(TEST_USER_ID))
                        .willReturn(references);
                given(userLinkRepository.countByUserIdAndStatus(TEST_USER_ID, LinkStatus.READ))
                        .willReturn(0L);
                given(userLinkRepository.countByUserIdAndStatus(TEST_USER_ID, LinkStatus.UNREAD))
                        .willReturn(0L);
                given(referenceUserLinkRepository.countUnreadByUserIdGroupByReference(TEST_USER_ID))
                        .willReturn(Collections.emptyList());
                given(userLinkRepository.findFirstCreatedDateByUserId(TEST_USER_ID))
                        .willReturn(null);

                // when
                UserStatResponse result = userStatService.getUserStat(TEST_USER_ID);

                // then
                assertThat(result.topReferences().references()).hasSize(3);
                assertThat(result.topReferences().references().get(0).title()).isEqualTo("개발");
                assertThat(result.topReferences().references().get(0).linkCount()).isEqualTo(5L);
                assertThat(result.topReferences().references().get(1).title()).isEqualTo("디자인");
                assertThat(result.topReferences().references().get(1).linkCount()).isEqualTo(3L);
                assertThat(result.topReferences().references().get(2).title()).isEqualTo("빈폴더");
                assertThat(result.topReferences().references().get(2).linkCount()).isEqualTo(0L);
                assertThat(result.topReferences().text()).isEqualTo("요즘 가장 많이 모아둔 폴더는 '개발'이에요. 링크가 총 5개예요.");
            }

            @Test
            @DisplayName("성공: 모든 폴더가 링크 0개인 경우 (2개)")
            void success_AllFoldersWithZeroLinks_Two() {
                // given
                List<ReferenceCategoryCountProjection> references = Arrays.asList(
                        new ReferenceCategoryCountProjection(1L, "개발", "#FF5733", 0L),
                        new ReferenceCategoryCountProjection(2L, "디자인", "#33FF57", 0L)
                );

                given(referenceUserLinkRepository.countByUserIdGroupByReference(TEST_USER_ID))
                        .willReturn(references);
                given(userLinkRepository.countByUserIdAndStatus(TEST_USER_ID, LinkStatus.READ))
                        .willReturn(0L);
                given(userLinkRepository.countByUserIdAndStatus(TEST_USER_ID, LinkStatus.UNREAD))
                        .willReturn(0L);
                given(referenceUserLinkRepository.countUnreadByUserIdGroupByReference(TEST_USER_ID))
                        .willReturn(Collections.emptyList());
                given(userLinkRepository.findFirstCreatedDateByUserId(TEST_USER_ID))
                        .willReturn(null);

                // when
                UserStatResponse result = userStatService.getUserStat(TEST_USER_ID);

                // then
                assertThat(result.topReferences().references()).hasSize(2);
                assertThat(result.topReferences().references().get(0).linkCount()).isEqualTo(0L);
                assertThat(result.topReferences().references().get(1).linkCount()).isEqualTo(0L);
                assertThat(result.topReferences().text()).isEqualTo("만들어둔 폴더가 아직 비어있어요. 관심 있는 링크를 모아보세요.");
            }

            @Test
            @DisplayName("성공: 모든 폴더가 링크 0개인 경우 (3개 이상)")
            void success_AllFoldersWithZeroLinks_Three() {
                // given
                List<ReferenceCategoryCountProjection> references = Arrays.asList(
                        new ReferenceCategoryCountProjection(1L, "개발", "#FF5733", 0L),
                        new ReferenceCategoryCountProjection(2L, "디자인", "#33FF57", 0L),
                        new ReferenceCategoryCountProjection(3L, "기획", "#5733FF", 0L)
                );

                given(referenceUserLinkRepository.countByUserIdGroupByReference(TEST_USER_ID))
                        .willReturn(references);
                given(userLinkRepository.countByUserIdAndStatus(TEST_USER_ID, LinkStatus.READ))
                        .willReturn(0L);
                given(userLinkRepository.countByUserIdAndStatus(TEST_USER_ID, LinkStatus.UNREAD))
                        .willReturn(0L);
                given(referenceUserLinkRepository.countUnreadByUserIdGroupByReference(TEST_USER_ID))
                        .willReturn(Collections.emptyList());
                given(userLinkRepository.findFirstCreatedDateByUserId(TEST_USER_ID))
                        .willReturn(null);

                // when
                UserStatResponse result = userStatService.getUserStat(TEST_USER_ID);

                // then
                assertThat(result.topReferences().references()).hasSize(3);
                assertThat(result.topReferences().references().get(0).linkCount()).isEqualTo(0L);
                assertThat(result.topReferences().references().get(1).linkCount()).isEqualTo(0L);
                assertThat(result.topReferences().references().get(2).linkCount()).isEqualTo(0L);
                assertThat(result.topReferences().text()).isEqualTo("만들어둔 폴더가 아직 비어있어요. 관심 있는 링크를 모아보세요.");
            }
        }

        @Nested
        @DisplayName("링크 열람 현황 테스트")
        class ReadStateTest {

            @Test
            @DisplayName("성공: 전체 링크 0개")
            void success_NoLinks() {
                // given
                given(referenceUserLinkRepository.countByUserIdGroupByReference(TEST_USER_ID))
                        .willReturn(Collections.emptyList());
                given(userLinkRepository.countByUserIdAndStatus(TEST_USER_ID, LinkStatus.READ))
                        .willReturn(0L);
                given(userLinkRepository.countByUserIdAndStatus(TEST_USER_ID, LinkStatus.UNREAD))
                        .willReturn(0L);
                given(referenceUserLinkRepository.countUnreadByUserIdGroupByReference(TEST_USER_ID))
                        .willReturn(Collections.emptyList());
                given(userLinkRepository.findFirstCreatedDateByUserId(TEST_USER_ID))
                        .willReturn(null);

                // when
                UserStatResponse result = userStatService.getUserStat(TEST_USER_ID);

                // then
                assertThat(result.readState().text()).isEqualTo("아직 저장한 링크가 없어요. 링크를 저장하고 열람 현황을 확인해보세요.");
            }

            @Test
            @DisplayName("성공: 전체 링크 1개 이상 + 미열람 0개")
            void success_AllRead() {
                // given
                given(referenceUserLinkRepository.countByUserIdGroupByReference(TEST_USER_ID))
                        .willReturn(Collections.emptyList());
                given(userLinkRepository.countByUserIdAndStatus(TEST_USER_ID, LinkStatus.READ))
                        .willReturn(10L);
                given(userLinkRepository.countByUserIdAndStatus(TEST_USER_ID, LinkStatus.UNREAD))
                        .willReturn(0L);
                given(referenceUserLinkRepository.countUnreadByUserIdGroupByReference(TEST_USER_ID))
                        .willReturn(Collections.emptyList());
                given(userLinkRepository.findFirstCreatedDateByUserId(TEST_USER_ID))
                        .willReturn(null);

                // when
                UserStatResponse result = userStatService.getUserStat(TEST_USER_ID);

                // then
                assertThat(result.readState().text()).isEqualTo("미열람 링크가 없어요. 저장한 링크를 모두 열람했어요.");
                assertThat(result.readState().readLinkCount()).isEqualTo(10L);
                assertThat(result.readState().unreadLinkCount()).isEqualTo(0L);
            }

            @Test
            @DisplayName("성공: 미열람 링크 있음 + 최다 폴더 단독")
            void success_UnreadWithSingleTopFolder() {
                // given
                List<ReferenceCategoryCountProjection> unreadByFolder = Arrays.asList(
                        new ReferenceCategoryCountProjection(1L, "개발", "#FF5733", 3L),
                        new ReferenceCategoryCountProjection(2L, "디자인", "#33FF57", 2L)
                );

                given(referenceUserLinkRepository.countByUserIdGroupByReference(TEST_USER_ID))
                        .willReturn(Collections.emptyList());
                given(userLinkRepository.countByUserIdAndStatus(TEST_USER_ID, LinkStatus.READ))
                        .willReturn(5L);
                given(userLinkRepository.countByUserIdAndStatus(TEST_USER_ID, LinkStatus.UNREAD))
                        .willReturn(5L);
                given(referenceUserLinkRepository.countUnreadByUserIdGroupByReference(TEST_USER_ID))
                        .willReturn(unreadByFolder);
                given(userLinkRepository.findFirstCreatedDateByUserId(TEST_USER_ID))
                        .willReturn(null);

                // when
                UserStatResponse result = userStatService.getUserStat(TEST_USER_ID);

                // then
                assertThat(result.readState().text()).isEqualTo("전체 링크의 50%를 아직 열람하지 않았어요. 미열람 링크는 '개발'에 총 3개로 가장 많아요.");
                assertThat(result.readState().unreadLinkPercent()).isEqualTo(50);
            }

            @Test
            @DisplayName("성공: 퍼센트 반올림 및 합이 100이 되도록 보정")
            void success_PercentCalculation() {
                // given
                given(referenceUserLinkRepository.countByUserIdGroupByReference(TEST_USER_ID))
                        .willReturn(Collections.emptyList());
                given(userLinkRepository.countByUserIdAndStatus(TEST_USER_ID, LinkStatus.READ))
                        .willReturn(3L);
                given(userLinkRepository.countByUserIdAndStatus(TEST_USER_ID, LinkStatus.UNREAD))
                        .willReturn(7L);
                given(referenceUserLinkRepository.countUnreadByUserIdGroupByReference(TEST_USER_ID))
                        .willReturn(Collections.emptyList());
                given(userLinkRepository.findFirstCreatedDateByUserId(TEST_USER_ID))
                        .willReturn(null);

                // when
                UserStatResponse result = userStatService.getUserStat(TEST_USER_ID);

                // then
                assertThat(result.readState().readLinkPercent()).isEqualTo(30);
                assertThat(result.readState().unreadLinkPercent()).isEqualTo(70);
                assertThat(result.readState().readLinkPercent() + result.readState().unreadLinkPercent()).isEqualTo(100);
            }

            @Test
            @DisplayName("성공: 미열람 최다 폴더 동률(2개)")
            void success_UnreadWithTwoTopFolders() {
                // given
                List<ReferenceCategoryCountProjection> unreadByFolder = Arrays.asList(
                        new ReferenceCategoryCountProjection(1L, "개발", "#FF5733", 5L),
                        new ReferenceCategoryCountProjection(2L, "디자인", "#33FF57", 5L),
                        new ReferenceCategoryCountProjection(3L, "기획", "#5733FF", 2L)
                );

                given(referenceUserLinkRepository.countByUserIdGroupByReference(TEST_USER_ID))
                        .willReturn(Collections.emptyList());
                given(userLinkRepository.countByUserIdAndStatus(TEST_USER_ID, LinkStatus.READ))
                        .willReturn(10L);
                given(userLinkRepository.countByUserIdAndStatus(TEST_USER_ID, LinkStatus.UNREAD))
                        .willReturn(12L);
                given(referenceUserLinkRepository.countUnreadByUserIdGroupByReference(TEST_USER_ID))
                        .willReturn(unreadByFolder);
                given(userLinkRepository.findFirstCreatedDateByUserId(TEST_USER_ID))
                        .willReturn(null);

                // when
                UserStatResponse result = userStatService.getUserStat(TEST_USER_ID);

                // then
                assertThat(result.readState().text()).contains("'개발'와 '디자인'에 각각 5개로 가장 많아요");
            }

            @Test
            @DisplayName("성공: 미열람 최다 폴더 동률(3개 이상)")
            void success_UnreadWithThreeOrMoreTopFolders() {
                // given
                List<ReferenceCategoryCountProjection> unreadByFolder = Arrays.asList(
                        new ReferenceCategoryCountProjection(1L, "개발", "#FF5733", 4L),
                        new ReferenceCategoryCountProjection(2L, "디자인", "#33FF57", 4L),
                        new ReferenceCategoryCountProjection(3L, "기획", "#5733FF", 4L)
                );

                given(referenceUserLinkRepository.countByUserIdGroupByReference(TEST_USER_ID))
                        .willReturn(Collections.emptyList());
                given(userLinkRepository.countByUserIdAndStatus(TEST_USER_ID, LinkStatus.READ))
                        .willReturn(8L);
                given(userLinkRepository.countByUserIdAndStatus(TEST_USER_ID, LinkStatus.UNREAD))
                        .willReturn(12L);
                given(referenceUserLinkRepository.countUnreadByUserIdGroupByReference(TEST_USER_ID))
                        .willReturn(unreadByFolder);
                given(userLinkRepository.findFirstCreatedDateByUserId(TEST_USER_ID))
                        .willReturn(null);

                // when
                UserStatResponse result = userStatService.getUserStat(TEST_USER_ID);

                // then
                assertThat(result.readState().text()).isEqualTo("전체 링크의 60%를 아직 열람하지 않았어요. 미열람 링크가 여러 폴더에 비슷하게 분포돼 있어요.");
            }

            @Test
            @DisplayName("성공: 미열람 있지만 폴더에 속하지 않은 경우")
            void success_UnreadWithoutFolder() {
                // given
                given(referenceUserLinkRepository.countByUserIdGroupByReference(TEST_USER_ID))
                        .willReturn(Collections.emptyList());
                given(userLinkRepository.countByUserIdAndStatus(TEST_USER_ID, LinkStatus.READ))
                        .willReturn(5L);
                given(userLinkRepository.countByUserIdAndStatus(TEST_USER_ID, LinkStatus.UNREAD))
                        .willReturn(5L);
                given(referenceUserLinkRepository.countUnreadByUserIdGroupByReference(TEST_USER_ID))
                        .willReturn(Collections.emptyList());
                given(userLinkRepository.findFirstCreatedDateByUserId(TEST_USER_ID))
                        .willReturn(null);

                // when
                UserStatResponse result = userStatService.getUserStat(TEST_USER_ID);

                // then
                assertThat(result.readState().text()).isEqualTo("전체 링크의 50%를 아직 열람하지 않았어요. 미열람 링크가 총 5개예요.");
            }
        }

        @Nested
        @DisplayName("링크 저장 패턴 테스트")
        class SavePatternTest {

            @Test
            @DisplayName("성공: 링크가 없는 경우")
            void success_NoLinks() {
                // given
                given(referenceUserLinkRepository.countByUserIdGroupByReference(TEST_USER_ID))
                        .willReturn(Collections.emptyList());
                given(userLinkRepository.countByUserIdAndStatus(TEST_USER_ID, LinkStatus.READ))
                        .willReturn(0L);
                given(userLinkRepository.countByUserIdAndStatus(TEST_USER_ID, LinkStatus.UNREAD))
                        .willReturn(0L);
                given(referenceUserLinkRepository.countUnreadByUserIdGroupByReference(TEST_USER_ID))
                        .willReturn(Collections.emptyList());
                given(userLinkRepository.findFirstCreatedDateByUserId(TEST_USER_ID))
                        .willReturn(null);

                // when
                UserStatResponse result = userStatService.getUserStat(TEST_USER_ID);

                // then
                assertThat(result.savePattern().peakDay()).isNull();
                assertThat(result.savePattern().text()).isEqualTo("최근 4주 동안 새로 저장한 링크가 없어요.");
            }

            @Test
            @DisplayName("성공: 링크 저장이 1주일 이하 + 단독 peakDay 있음")
            void success_LessThanWeek() {
                // given
                LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
                List<DayCountProjection> dayCounts = Arrays.asList(
                        createDayCount(2, 5L),  // 월요일 (최다)
                        createDayCount(3, 2L)   // 화요일
                );

                given(referenceUserLinkRepository.countByUserIdGroupByReference(TEST_USER_ID))
                        .willReturn(Collections.emptyList());
                given(userLinkRepository.countByUserIdAndStatus(TEST_USER_ID, LinkStatus.READ))
                        .willReturn(0L);
                given(userLinkRepository.countByUserIdAndStatus(TEST_USER_ID, LinkStatus.UNREAD))
                        .willReturn(5L);
                given(referenceUserLinkRepository.countUnreadByUserIdGroupByReference(TEST_USER_ID))
                        .willReturn(Collections.emptyList());
                given(userLinkRepository.findFirstCreatedDateByUserId(TEST_USER_ID))
                        .willReturn(threeDaysAgo);
                given(userLinkRepository.countByUserIdGroupByDayOfWeek(eq(TEST_USER_ID), any(LocalDateTime.class)))
                        .willReturn(dayCounts);

                // when
                UserStatResponse result = userStatService.getUserStat(TEST_USER_ID);

                // then
                assertThat(result.savePattern().peakDay()).isEqualTo("월");
                assertThat(result.savePattern().text()).isEqualTo("아직 링크를 저장한 지 일주일이 안 됐어요. 일주일이 지나면 패턴을 보여드릴게요.");
            }

            @Test
            @DisplayName("성공: 링크 저장이 1주일 이하 + peakDay 동률")
            void success_LessThanWeek_TiePeakDay() {
                // given
                LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
                List<DayCountProjection> dayCounts = Arrays.asList(
                        createDayCount(2, 3L),  // 월요일 (동률)
                        createDayCount(3, 3L)   // 화요일 (동률)
                );

                given(referenceUserLinkRepository.countByUserIdGroupByReference(TEST_USER_ID))
                        .willReturn(Collections.emptyList());
                given(userLinkRepository.countByUserIdAndStatus(TEST_USER_ID, LinkStatus.READ))
                        .willReturn(0L);
                given(userLinkRepository.countByUserIdAndStatus(TEST_USER_ID, LinkStatus.UNREAD))
                        .willReturn(5L);
                given(referenceUserLinkRepository.countUnreadByUserIdGroupByReference(TEST_USER_ID))
                        .willReturn(Collections.emptyList());
                given(userLinkRepository.findFirstCreatedDateByUserId(TEST_USER_ID))
                        .willReturn(threeDaysAgo);
                given(userLinkRepository.countByUserIdGroupByDayOfWeek(eq(TEST_USER_ID), any(LocalDateTime.class)))
                        .willReturn(dayCounts);

                // when
                UserStatResponse result = userStatService.getUserStat(TEST_USER_ID);

                // then
                assertThat(result.savePattern().peakDay()).isNull();
                assertThat(result.savePattern().text()).isEqualTo("아직 링크를 저장한 지 일주일이 안 됐어요. 일주일이 지나면 패턴을 보여드릴게요.");
            }

            @Test
            @DisplayName("성공: 링크 저장이 1주일 이상 + 최다 요일 단독")
            void success_SinglePeakDay() {
                // given
                LocalDateTime twoWeeksAgo = LocalDateTime.now().minusWeeks(2);
                List<DayCountProjection> dayCounts = Arrays.asList(
                        createDayCount(1, 2L),  // 일요일
                        createDayCount(2, 10L), // 월요일 (최다)
                        createDayCount(3, 5L),  // 화요일
                        createDayCount(4, 4L),  // 수요일
                        createDayCount(5, 6L),  // 목요일
                        createDayCount(6, 3L),  // 금요일
                        createDayCount(7, 4L)   // 토요일
                );

                given(referenceUserLinkRepository.countByUserIdGroupByReference(TEST_USER_ID))
                        .willReturn(Collections.emptyList());
                given(userLinkRepository.countByUserIdAndStatus(TEST_USER_ID, LinkStatus.READ))
                        .willReturn(0L);
                given(userLinkRepository.countByUserIdAndStatus(TEST_USER_ID, LinkStatus.UNREAD))
                        .willReturn(0L);
                given(referenceUserLinkRepository.countUnreadByUserIdGroupByReference(TEST_USER_ID))
                        .willReturn(Collections.emptyList());
                given(userLinkRepository.findFirstCreatedDateByUserId(TEST_USER_ID))
                        .willReturn(twoWeeksAgo);
                given(userLinkRepository.countByUserIdGroupByDayOfWeek(eq(TEST_USER_ID), any(LocalDateTime.class)))
                        .willReturn(dayCounts);

                // when
                UserStatResponse result = userStatService.getUserStat(TEST_USER_ID);

                // then
                assertThat(result.savePattern().peakDay()).isEqualTo("월");
                assertThat(result.savePattern().text()).isEqualTo("'월요일'에 링크를 가장 많이 저장했어요. 총 10개예요.");
            }

            @Test
            @DisplayName("성공: 링크 저장이 1주일 이상 + 최다 요일 동률(2개)")
            void success_TwoPeakDays() {
                // given
                LocalDateTime twoWeeksAgo = LocalDateTime.now().minusWeeks(2);
                List<DayCountProjection> dayCounts = Arrays.asList(
                        createDayCount(1, 2L),  // 일요일
                        createDayCount(2, 8L),  // 월요일 (동률)
                        createDayCount(3, 8L),  // 화요일 (동률)
                        createDayCount(4, 4L),  // 수요일
                        createDayCount(5, 5L),  // 목요일
                        createDayCount(6, 3L),  // 금요일
                        createDayCount(7, 4L)   // 토요일
                );

                given(referenceUserLinkRepository.countByUserIdGroupByReference(TEST_USER_ID))
                        .willReturn(Collections.emptyList());
                given(userLinkRepository.countByUserIdAndStatus(TEST_USER_ID, LinkStatus.READ))
                        .willReturn(0L);
                given(userLinkRepository.countByUserIdAndStatus(TEST_USER_ID, LinkStatus.UNREAD))
                        .willReturn(0L);
                given(referenceUserLinkRepository.countUnreadByUserIdGroupByReference(TEST_USER_ID))
                        .willReturn(Collections.emptyList());
                given(userLinkRepository.findFirstCreatedDateByUserId(TEST_USER_ID))
                        .willReturn(twoWeeksAgo);
                given(userLinkRepository.countByUserIdGroupByDayOfWeek(eq(TEST_USER_ID), any(LocalDateTime.class)))
                        .willReturn(dayCounts);

                // when
                UserStatResponse result = userStatService.getUserStat(TEST_USER_ID);

                // then
                assertThat(result.savePattern().peakDay()).isNull();
                assertThat(result.savePattern().text()).isEqualTo("'월요일'과 '화요일'에 링크를 가장 많이 저장했어요. 각각 8개예요.");
            }

            @Test
            @DisplayName("성공: 요일별 저장이 비슷할 때 (차이 3개 이하, 14개 이상)")
            void success_SimilarPattern() {
                // given
                LocalDateTime twoWeeksAgo = LocalDateTime.now().minusWeeks(2);
                List<DayCountProjection> dayCounts = Arrays.asList(
                        createDayCount(1, 3L),  // 일요일
                        createDayCount(2, 4L),  // 월요일
                        createDayCount(3, 3L),  // 화요일
                        createDayCount(4, 5L),  // 수요일 (최다)
                        createDayCount(5, 4L),  // 목요일
                        createDayCount(6, 3L),  // 금요일
                        createDayCount(7, 4L)   // 토요일
                        // 합계: 26개, 최다-최소 차이: 2개
                );

                given(referenceUserLinkRepository.countByUserIdGroupByReference(TEST_USER_ID))
                        .willReturn(Collections.emptyList());
                given(userLinkRepository.countByUserIdAndStatus(TEST_USER_ID, LinkStatus.READ))
                        .willReturn(0L);
                given(userLinkRepository.countByUserIdAndStatus(TEST_USER_ID, LinkStatus.UNREAD))
                        .willReturn(0L);
                given(referenceUserLinkRepository.countUnreadByUserIdGroupByReference(TEST_USER_ID))
                        .willReturn(Collections.emptyList());
                given(userLinkRepository.findFirstCreatedDateByUserId(TEST_USER_ID))
                        .willReturn(twoWeeksAgo);
                given(userLinkRepository.countByUserIdGroupByDayOfWeek(eq(TEST_USER_ID), any(LocalDateTime.class)))
                        .willReturn(dayCounts);

                // when
                UserStatResponse result = userStatService.getUserStat(TEST_USER_ID);

                // then
                assertThat(result.savePattern().text()).isEqualTo("요일에 상관없이 꾸준히 링크를 저장하고 있어요.");
            }

            @Test
            @DisplayName("성공: 최다 요일이 3개 이상 동률이면 비슷함으로 판단")
            void success_SimilarPattern_Tie3Plus() {
                // given
                LocalDateTime twoWeeksAgo = LocalDateTime.now().minusWeeks(2);
                List<DayCountProjection> dayCounts = Arrays.asList(
                        createDayCount(1, 5L),  // 일요일
                        createDayCount(2, 7L),  // 월요일 (동률)
                        createDayCount(3, 7L),  // 화요일 (동률)
                        createDayCount(4, 7L),  // 수요일 (동률)
                        createDayCount(5, 6L),  // 목요일
                        createDayCount(6, 5L),  // 금요일
                        createDayCount(7, 6L)   // 토요일
                );

                given(referenceUserLinkRepository.countByUserIdGroupByReference(TEST_USER_ID))
                        .willReturn(Collections.emptyList());
                given(userLinkRepository.countByUserIdAndStatus(TEST_USER_ID, LinkStatus.READ))
                        .willReturn(0L);
                given(userLinkRepository.countByUserIdAndStatus(TEST_USER_ID, LinkStatus.UNREAD))
                        .willReturn(0L);
                given(referenceUserLinkRepository.countUnreadByUserIdGroupByReference(TEST_USER_ID))
                        .willReturn(Collections.emptyList());
                given(userLinkRepository.findFirstCreatedDateByUserId(TEST_USER_ID))
                        .willReturn(twoWeeksAgo);
                given(userLinkRepository.countByUserIdGroupByDayOfWeek(eq(TEST_USER_ID), any(LocalDateTime.class)))
                        .willReturn(dayCounts);

                // when
                UserStatResponse result = userStatService.getUserStat(TEST_USER_ID);

                // then
                assertThat(result.savePattern().text()).isEqualTo("요일에 상관없이 꾸준히 링크를 저장하고 있어요.");
            }

            @Test
            @DisplayName("성공: 최근 4주 동안 저장이 0개일 때")
            void success_NoSavedInLast4Weeks() {
                // given
                LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);

                given(referenceUserLinkRepository.countByUserIdGroupByReference(TEST_USER_ID))
                        .willReturn(Collections.emptyList());
                given(userLinkRepository.countByUserIdAndStatus(TEST_USER_ID, LinkStatus.READ))
                        .willReturn(0L);
                given(userLinkRepository.countByUserIdAndStatus(TEST_USER_ID, LinkStatus.UNREAD))
                        .willReturn(0L);
                given(referenceUserLinkRepository.countUnreadByUserIdGroupByReference(TEST_USER_ID))
                        .willReturn(Collections.emptyList());
                given(userLinkRepository.findFirstCreatedDateByUserId(TEST_USER_ID))
                        .willReturn(oneMonthAgo);
                given(userLinkRepository.countByUserIdGroupByDayOfWeek(eq(TEST_USER_ID), any(LocalDateTime.class)))
                        .willReturn(Collections.emptyList());

                // when
                UserStatResponse result = userStatService.getUserStat(TEST_USER_ID);

                // then
                assertThat(result.savePattern().peakDay()).isNull();
                assertThat(result.savePattern().text()).isEqualTo("최근 4주 동안 새로 저장한 링크가 없어요.");
            }

            @Test
            @DisplayName("성공: 요일별 개수 리스트가 일~토 순서로 반환")
            void success_DayCountsOrderSundayToSaturday() {
                // given
                LocalDateTime twoWeeksAgo = LocalDateTime.now().minusWeeks(2);
                List<DayCountProjection> dayCounts = Arrays.asList(
                        createDayCount(1, 2L),
                        createDayCount(2, 10L),
                        createDayCount(3, 5L),
                        createDayCount(4, 4L),
                        createDayCount(5, 6L),
                        createDayCount(6, 3L),
                        createDayCount(7, 4L)
                );

                given(referenceUserLinkRepository.countByUserIdGroupByReference(TEST_USER_ID))
                        .willReturn(Collections.emptyList());
                given(userLinkRepository.countByUserIdAndStatus(TEST_USER_ID, LinkStatus.READ))
                        .willReturn(0L);
                given(userLinkRepository.countByUserIdAndStatus(TEST_USER_ID, LinkStatus.UNREAD))
                        .willReturn(0L);
                given(referenceUserLinkRepository.countUnreadByUserIdGroupByReference(TEST_USER_ID))
                        .willReturn(Collections.emptyList());
                given(userLinkRepository.findFirstCreatedDateByUserId(TEST_USER_ID))
                        .willReturn(twoWeeksAgo);
                given(userLinkRepository.countByUserIdGroupByDayOfWeek(eq(TEST_USER_ID), any(LocalDateTime.class)))
                        .willReturn(dayCounts);

                // when
                UserStatResponse result = userStatService.getUserStat(TEST_USER_ID);

                // then
                assertThat(result.savePattern().counts()).hasSize(7);
                assertThat(result.savePattern().counts().get(0).day()).isEqualTo("월");
                assertThat(result.savePattern().counts().get(1).day()).isEqualTo("화");
                assertThat(result.savePattern().counts().get(6).day()).isEqualTo("일");
            }
        }
    }

    /**
     * DayCountProjection 헬퍼 메서드
     */
    private static DayCountProjection createDayCount(Integer dayOfWeek, Long count) {
        return new DayCountProjection() {
            @Override
            public Integer getDayOfWeek() {
                return dayOfWeek;
            }

            @Override
            public Long getCount() {
                return count;
            }
        };
    }
}