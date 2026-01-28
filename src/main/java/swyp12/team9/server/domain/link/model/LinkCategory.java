package swyp12.team9.server.domain.link.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;

/**
 * 탐색 탭의 고정 카테고리 (검색어로 사용)
 * DB에 저장되지 않으며, Elasticsearch 검색 키워드로만 활용
 */
@Getter
@RequiredArgsConstructor
public enum LinkCategory {
    ECONOMY_CURRENT_AFFAIRS("경제/시사"),
    BEAUTY_FASHION("뷰티/패션"),
    FOOD("요리/식품"),
    HEALTH_FITNESS("운동/건강"),
    HUMANITIES("인문/지식"),
    CAREER("직장/자기개발"),
    HOME_LIVING("홈/리빙"),
    ETC("기타");

    private final String displayName;

    /**
     * displayName으로 Enum 찾기
     */
    public static LinkCategory fromDisplayName(String displayName) {
        for (LinkCategory category : values()) {
            if (category.displayName.equals(displayName)) {
                return category;
            }
        }
        return null;
    }

    /**
     * 모든 카테고리의 displayName 목록 반환
     */
    public static List<String> getAllDisplayNames() {
        return Arrays.stream(values())
                .map(LinkCategory::getDisplayName)
                .toList();
    }
}
