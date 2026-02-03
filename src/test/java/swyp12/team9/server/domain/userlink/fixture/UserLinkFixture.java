package swyp12.team9.server.domain.userlink.fixture;

import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.user.model.User;
import swyp12.team9.server.domain.userlink.model.LinkStatus;
import swyp12.team9.server.domain.userlink.model.UserLink;

public class UserLinkFixture {

    public static final Long USER_LINK_ID = 1L;
    public static final String WHY = "유용한 정보라서 저장했습니다.";
    public static final String MEMO = "나중에 참고하기 위한 메모입니다.";
    public static final Boolean IS_PUBLIC = false;
    public static final Long VIEW_COUNT = 0L;

    public static final String UPDATED_WHY = "수정된 저장 이유입니다.";
    public static final String UPDATED_MEMO = "수정된 메모입니다.";

    public static final String URL = "https://example.com";
    public static final String LINK_TITLE = "테스트 링크 제목";
    public static final String LINK_DESCRIPTION = "테스트 링크 설명";

    /**
     * 기본 UserLink 엔티티 생성
     */
    public static UserLink createUserLink(User user, Link link) {
        return UserLink.builder()
                .user(user)
                .link(link)
                .why(WHY)
                .memo(MEMO)
                .build();
    }

    /**
     * 기본 Link 엔티티 생성
     */
    public static Link createLink() {
        return Link.builder()
                .url(URL)
                .title(LINK_TITLE)
                .description(LINK_DESCRIPTION)
                .build();
    }

    /**
     * ID가 설정된 Link 생성 (테스트용)
     */
    public static Link createLinkWithId(Long id) {
        return new Link(
                id,
                URL,
                LINK_TITLE,
                LINK_DESCRIPTION,
                null,
                null,
                null
        );
    }

    /**
     * 커스텀 URL로 Link 생성
     */
    public static Link createLinkWithUrl(String url) {
        return Link.builder()
                .url(url)
                .title(LINK_TITLE)
                .description(LINK_DESCRIPTION)
                .build();
    }

    /**
     * 커스텀 속성으로 UserLink 생성
     */
    public static UserLink createUserLinkWithCustom(User user, Link link, String why, String memo) {
        return UserLink.builder()
                .user(user)
                .link(link)
                .why(why)
                .memo(memo)
                .build();
    }
}