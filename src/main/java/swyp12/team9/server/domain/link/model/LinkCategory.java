package swyp12.team9.server.domain.link.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 탐색 탭의 고정 카테고리 (검색어로 사용)
 * DB에 저장되지 않으며, Elasticsearch 검색 키워드로만 활용
 */
@Getter
@RequiredArgsConstructor
public enum LinkCategory {
    ECONOMY_CURRENT_AFFAIRS("경제/시사"),
    BEAUTY_FASHION("뷰티/패션"),
    FOOD("푸드"),
    TRAVEL("여행"),
    SPORTS_LEISURE("스포츠/레저"),
    CULTURE_ART("문화/예술"),
    TECH_IT("테크/IT"),
    LIFE_HEALTH("라이프/건강"),
    EDUCATION_CAREER("교육/커리어"),
    ENTERTAINMENT("엔터테인먼트");

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
}
