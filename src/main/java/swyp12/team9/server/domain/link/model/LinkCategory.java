package swyp12.team9.server.domain.link.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 링크 카테고리 (고정 enum)
 */
@Getter
@RequiredArgsConstructor
public enum LinkCategory {

    ECONOMY_CURRENT("경제/시사"),
    BEAUTY_FASHION("뷰티/패션"),
    FOOD_COOKING("요리/식품"),
    HEALTH_FITNESS("운동/건강"),
    HUMANITIES_KNOWLEDGE("인문/지식"),
    CAREER_SELF_DEV("직장/자기개발"),
    HOME_LIVING("홈/리빙");

    private final String displayName;

    public static LinkCategory fromDisplayName(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return null;
        }
        for (LinkCategory category : values()) {
            if (category.displayName.equals(displayName)) {
                return category;
            }
        }
        return null;
    }
}
