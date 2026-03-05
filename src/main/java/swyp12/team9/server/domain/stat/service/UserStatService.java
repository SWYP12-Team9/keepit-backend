package swyp12.team9.server.domain.stat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swyp12.team9.server.domain.stat.dto.*;
import swyp12.team9.server.domain.reference.relation.dto.ReferenceCategoryCountProjection;
import swyp12.team9.server.domain.reference.relation.repository.ReferenceUserLinkRepository;
import swyp12.team9.server.domain.userlink.dto.DayCountProjection;
import swyp12.team9.server.domain.userlink.model.LinkStatus;
import swyp12.team9.server.domain.userlink.repository.UserLinkRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static swyp12.team9.server.domain.stat.dto.DayOfWeek.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserStatService {

    private final UserLinkRepository userLinkRepository;
    private final ReferenceUserLinkRepository referenceUserLinkRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    /**
     * 사용자 통계 조회
     * @param userId 사용자 ID
     * @return 사용자 통계 응답
     */
    public UserStatResponse getUserStat(Long userId) {

        String todayDate = LocalDate.now().format(DATE_FORMATTER);

        TopReferencesResponse topReferences = getTopReferences(userId, todayDate);
        ReadStateResponse readState = getReadState(userId, todayDate);
        SavePatternResponse savePattern = getSavePattern(userId);

        return UserStatResponse.of(topReferences, readState, savePattern);
    }

    /**
     * 상위 레퍼런스 통계 조회 (최대 6개, 동률 처리)
     * isDefault=true인 미지정 폴더는 제외
     */
    private TopReferencesResponse getTopReferences(Long userId, String todayDate) {
        List<ReferenceCategoryCountProjection> results = referenceUserLinkRepository.countByUserIdGroupByReference(userId);

        String insight = generateTopReferencesInsight(results);

        // 상위 6개 추출 및 변환
        AtomicInteger rankCounter = new AtomicInteger(1);
        List<TopReferenceInfo> references = results.stream()
                .limit(6)
                .map(projection -> TopReferenceInfo.of(rankCounter.getAndIncrement(), projection))
                .collect(Collectors.toList());

        return TopReferencesResponse.of(todayDate, references, insight);
    }

    /**
     * 인기 레퍼런스 인사이트 생성
     */
    private String generateTopReferencesInsight(List<ReferenceCategoryCountProjection> results) {
        if (results.isEmpty()) {
            return "아직 레퍼런스 폴더가 없어요. 폴더를 만들고 링크를 모아보세요.";
        }

        // 1위 개수
        long topCount = results.getFirst().getCount();

        // 모든 폴더의 링크 수가 0개인 경우 (커스텀 폴더는 있지만 비어있음)
        if (topCount == 0) {
            return "만들어둔 폴더가 아직 비어있어요. 관심 있는 링크를 모아보세요.";
        }

        // 1위 동률인 폴더들 찾기
        List<ReferenceCategoryCountProjection> topReferences = results.stream()
                .filter(r -> r.getCount() == topCount)
                .toList();

        if (topReferences.size() == 1) {
            // 1위 단독
            String folderName = topReferences.getFirst().getReferenceTitle();
            return String.format("요즘 가장 많이 모아둔 폴더는 '%s'이에요. 링크가 총 %d개예요.", folderName, topCount);
        } else if (topReferences.size() == 2) {
            // 1위 동률 2개
            String folder1 = topReferences.get(0).getReferenceTitle();
            String folder2 = topReferences.get(1).getReferenceTitle();
            return String.format("요즘 가장 많이 모아둔 폴더는 '%s'와 '%s'예요. 링크가 각각 %d개예요.", folder1, folder2, topCount);
        } else {
            // 1위 동률 3개 이상
            return String.format("요즘 가장 많이 모아둔 폴더가 여러 개예요. 상위 폴더들이 각각 %d개예요.", topCount);
        }
    }


    /**
     * 읽음 상태 통계 조회 (미열람 중심, 퍼센트 반올림)
     * 미열람 폴더별 집계 시 isDefault 포함 모든 레퍼런스 대상
     */
    private ReadStateResponse getReadState(Long userId, String todayDate) {
        long readCount = userLinkRepository.countByUserIdAndStatus(userId, LinkStatus.READ);
        long unreadCount = userLinkRepository.countByUserIdAndStatus(userId, LinkStatus.UNREAD);
        long totalCount = readCount + unreadCount;

        // 퍼센트 계산 (반올림 후 합이 100이 되도록 보정)
        int[] percents = calculatePercents(readCount, unreadCount, totalCount);
        int readPercent = percents[0];
        int unreadPercent = percents[1];

        // 미열람 폴더별 집계
        List<ReferenceCategoryCountProjection> unreadByFolder =
                referenceUserLinkRepository.countUnreadByUserIdGroupByReference(userId);

        String insight = generateReadStateInsight(totalCount, unreadCount, unreadPercent, unreadByFolder);

        return ReadStateResponse.of(todayDate, readCount, unreadCount, readPercent, unreadPercent, insight);
    }

    /**
     * 퍼센트 계산 (반올림 후 합이 100이 되도록 보정)
     */
    private int[] calculatePercents(long readCount, long unreadCount, long totalCount) {
        if (totalCount == 0) {
            return new int[]{0, 0};
        }

        int readPercent = (int) Math.round((double) readCount / totalCount * 100);
        int unreadPercent = (int) Math.round((double) unreadCount / totalCount * 100);

        // 합이 100이 되도록 보정
        int sum = readPercent + unreadPercent;
        if (sum != 100) {
            if (readCount >= unreadCount) {
                readPercent += (100 - sum);
            } else {
                unreadPercent += (100 - sum);
            }
        }

        return new int[]{readPercent, unreadPercent};
    }

    /**
     * 읽음 상태 인사이트 생성 (미열람 중심)
     */
    private String generateReadStateInsight(long totalCount, long unreadCount, int unreadPercent,
                                           List<ReferenceCategoryCountProjection> unreadByFolder) {
        // 전체 링크 0개
        if (totalCount == 0) {
            return "아직 저장한 링크가 없어요. 링크를 저장하고 열람 현황을 확인해보세요.";
        }

        // 전체 링크 1개 이상 + 미열람 0개
        if (unreadCount == 0) {
            return "미열람 링크가 없어요. 저장한 링크를 모두 열람했어요.";
        }

        // 전체 링크 1개 이상 + 미열람 1개 이상
        if (unreadByFolder.isEmpty()) {
            // 미열람 최다 폴더 없음 (폴더에 속하지 않은 링크만 있음)
            return String.format("전체 링크의 %d%%를 아직 열람하지 않았어요. 미열람 링크가 총 %d개예요.", unreadPercent, unreadCount);
        }

        // 미열람 최다 개수
        long maxUnreadCount = unreadByFolder.getFirst().getCount();

        // 미열람 최다 폴더들 찾기
        List<ReferenceCategoryCountProjection> topUnreadFolders = unreadByFolder.stream()
                .filter(f -> f.getCount() == maxUnreadCount)
                .toList();

        if (topUnreadFolders.size() == 1) {
            // 미열람 최다 폴더 단독
            String folderName = topUnreadFolders.getFirst().getReferenceTitle();
            return String.format("전체 링크의 %d%%를 아직 열람하지 않았어요. 미열람 링크는 '%s'에 총 %d개로 가장 많아요.",
                    unreadPercent, folderName, maxUnreadCount);
        } else if (topUnreadFolders.size() == 2) {
            // 미열람 최다 폴더 동률 2개
            String folder1 = topUnreadFolders.get(0).getReferenceTitle();
            String folder2 = topUnreadFolders.get(1).getReferenceTitle();
            return String.format("전체 링크의 %d%%를 아직 열람하지 않았어요. 미열람 링크는 '%s'와 '%s'에 각각 %d개로 가장 많아요.",
                    unreadPercent, folder1, folder2, maxUnreadCount);
        } else {
            // 미열람 최다 폴더 동률 3개 이상
            return String.format("전체 링크의 %d%%를 아직 열람하지 않았어요. 미열람 링크가 여러 폴더에 비슷하게 분포돼 있어요.", unreadPercent);
        }
    }

    /**
     * 저장 패턴 통계 조회 (최근 4주 이하 데이터 기준)
     * peakDay는 단독 1위 요일만 반환, 동률이거나 데이터 없으면 null
     */
    private SavePatternResponse getSavePattern(Long userId) {
        LocalDate today = LocalDate.now();

        // 사용자가 처음 링크를 저장한 날짜 조회
        LocalDateTime firstSavedDate = userLinkRepository.findFirstCreatedDateByUserId(userId);

        // 링크가 없는 경우
        if (firstSavedDate == null) {
            return buildEmptySavePattern(today);
        }

        LocalDate firstDate = firstSavedDate.toLocalDate();
        long daysSinceFirst = ChronoUnit.DAYS.between(firstDate, today);

        // 1주일 이하인 경우
        if (daysSinceFirst < 7) {
            return buildLessThanWeekSavePattern(firstDate, today, userId);
        }

        // 최근 4주 데이터 수집 (첫 저장일이 4주 이내면 첫 저장일부터)
        LocalDate startDate = today.minusWeeks(4);
        if (firstDate.isAfter(startDate)) {
            startDate = firstDate;
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        List<DayCountProjection> results = userLinkRepository.countByUserIdGroupByDayOfWeek(userId, startDateTime);

        List<DayLinkCountResponse> counts = buildDailyCounts(results);
        String period = String.format("%s ~ %s", startDate.format(DATE_FORMATTER), today.format(DATE_FORMATTER));

        long totalCount = counts.stream().mapToLong(DayLinkCountResponse::linkCount).sum();

        // 최근 4주 동안 저장이 0개
        if (totalCount == 0) {
            return SavePatternResponse.of(period, null, counts, "최근 4주 동안 새로 저장한 링크가 없어요.");
        }

        // 최다 저장 요일 분석
        String insight = generateSavePatternInsight(counts, totalCount);
        String peakDay = extractPeakDay(counts);

        return SavePatternResponse.of(period, peakDay, counts, insight);
    }

    /**
     * 빈 저장 패턴 응답 생성 (링크가 없는 경우)
     */
    private SavePatternResponse buildEmptySavePattern(LocalDate today) {
        LocalDate startDate = today.minusWeeks(4);
        String period = String.format("%s ~ %s", startDate.format(DATE_FORMATTER), today.format(DATE_FORMATTER));

        List<DayLinkCountResponse> emptyCounts = Stream.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY)
                .map(day -> DayLinkCountResponse.of(day.getKoreanName(), 0L))
                .collect(Collectors.toList());

        return SavePatternResponse.of(period, null, emptyCounts, "최근 4주 동안 새로 저장한 링크가 없어요.");
    }

    /**
     * 1주일 이하 저장 패턴 응답 생성
     * 실제 데이터를 조회하고 peakDay도 계산하되, 인사이트는 "일주일이 안 됐어요" 메시지 표시
     */
    private SavePatternResponse buildLessThanWeekSavePattern(LocalDate firstDate, LocalDate today, Long userId) {
        String period = String.format("%s ~ %s", firstDate.format(DATE_FORMATTER), today.format(DATE_FORMATTER));

        // 실제 저장된 링크 데이터 조회
        LocalDateTime startDateTime = firstDate.atStartOfDay();
        List<DayCountProjection> results = userLinkRepository.countByUserIdGroupByDayOfWeek(userId, startDateTime);
        List<DayLinkCountResponse> counts = buildDailyCounts(results);

        // peakDay 계산 (단독 1위만 반환)
        String peakDay = extractPeakDay(counts);

        return SavePatternResponse.of(period, peakDay, counts, "아직 링크를 저장한 지 일주일이 안 됐어요. 일주일이 지나면 패턴을 보여드릴게요.");
    }

    /**
     * 최다 저장 요일 추출
     * 동률이면 null 반환
     */
    private String extractPeakDay(List<DayLinkCountResponse> counts) {
        // 최대 링크 개수 찾기
        long maxCount = counts.stream()
                .mapToLong(DayLinkCountResponse::linkCount)
                .max()
                .orElse(0L);

        // 최대 개수인 요일들 찾기
        List<DayLinkCountResponse> topDays = counts.stream()
                .filter(c -> c.linkCount() == maxCount)
                .toList();

        // 동률이면 null 반환, 단독이면 요일 이름 반환
        return topDays.size() == 1 ? topDays.getFirst().day() : null;
    }

    /**
     * 저장 패턴 인사이트 생성
     */
    private String generateSavePatternInsight(List<DayLinkCountResponse> counts, long totalCount) {
        // 최다 저장 개수
        long maxCount = counts.stream()
                .mapToLong(DayLinkCountResponse::linkCount)
                .max()
                .orElse(0L);

        // 최소 저장 개수
        long minCount = counts.stream()
                .mapToLong(DayLinkCountResponse::linkCount)
                .min()
                .orElse(0L);

        // 최다 요일들 찾기
        List<DayLinkCountResponse> topDays = counts.stream()
                .filter(c -> c.linkCount() == maxCount)
                .toList();

        // "비슷함" 판단: 최근 4주 저장 링크가 14개 이상이고, 최다/최소 차이가 3개 이하이거나 최다 요일이 3개 이상 동률
        boolean isSimilar = totalCount >= 14 &&
                ((maxCount - minCount) <= 3 || topDays.size() >= 3);

        if (isSimilar) {
            return "요일에 상관없이 꾸준히 링크를 저장하고 있어요.";
        }

        if (topDays.size() == 1) {
            // 최다 요일 단독
            String dayName = topDays.getFirst().day();
            return String.format("'%s요일'에 링크를 가장 많이 저장했어요. 총 %d개예요.", dayName, maxCount);
        } else if (topDays.size() == 2) {
            // 최다 요일 동률 2개
            String day1 = topDays.get(0).day();
            String day2 = topDays.get(1).day();
            return String.format("'%s요일'과 '%s요일'에 링크를 가장 많이 저장했어요. 각각 %d개예요.", day1, day2, maxCount);
        } else {
            // 최다 요일 동률 3개 이상 -> "비슷함"으로 처리
            return "요일에 상관없이 꾸준히 링크를 저장하고 있어요.";
        }
    }

    /**
     * 요일별 개수 리스트 생성 (월요일~일요일 순서)
     */
    private List<DayLinkCountResponse> buildDailyCounts(List<DayCountProjection> results) {
        Map<Integer, Long> countMap = results.stream()
                .collect(Collectors.toMap(
                        DayCountProjection::getDayOfWeek,
                        DayCountProjection::getCount
                ));

        // 월(2) ~ 일(1) 순서
        return Stream.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY)
                .map(day -> DayLinkCountResponse.of(
                        day.getKoreanName(),
                        countMap.getOrDefault(day.getMysqlDayOfWeek(), 0L)
                ))
                .collect(Collectors.toList());
    }
}