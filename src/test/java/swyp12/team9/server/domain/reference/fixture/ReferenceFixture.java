package swyp12.team9.server.domain.reference.fixture;

import swyp12.team9.server.domain.reference.model.Reference;
import swyp12.team9.server.domain.user.model.User;

public class ReferenceFixture {

    public static final Long REFERENCE_ID = 1L;
    public static final String TITLE = "테스트 레퍼런스";
    public static final String DESCRIPTION = "테스트 설명입니다.";
    public static final Boolean IS_PUBLIC = true;
    public static final String COLOR_CODE = "#FF5733";
    public static final Boolean IS_DEFAULT = false;

    public static final String UPDATED_TITLE = "수정된 레퍼런스";
    public static final String UPDATED_DESCRIPTION = "수정된 설명입니다.";
    public static final String UPDATED_COLOR_CODE = "#33FF57";

    /**
     * 기본 Reference 엔티티 생성
     */
    public static Reference createReference(User user) {
        return Reference.builder()
                .user(user)
                .title(TITLE)
                .description(DESCRIPTION)
                .isPublic(IS_PUBLIC)
                .colorCode(COLOR_CODE)
                .isDefault(IS_DEFAULT)
                .build();
    }

    /**
     * 공개 Reference 생성
     */
    public static Reference createPublicReference(User user) {
        return Reference.builder()
                .user(user)
                .title(TITLE)
                .description(DESCRIPTION)
                .isPublic(true)
                .colorCode(COLOR_CODE)
                .isDefault(false)
                .build();
    }

    /**
     * 비공개 Reference 생성
     */
    public static Reference createPrivateReference(User user) {
        return Reference.builder()
                .user(user)
                .title(TITLE)
                .description(DESCRIPTION)
                .isPublic(false)
                .colorCode(COLOR_CODE)
                .isDefault(false)
                .build();
    }

    /**
     * 기본 미지정 폴더 생성
     */
    public static Reference createDefaultReference(User user) {
        return Reference.builder()
                .user(user)
                .title("미지정")
                .description(null)
                .isPublic(false)
                .colorCode(null)
                .isDefault(true)
                .build();
    }

    /**
     * ID가 설정된 Reference 생성 (테스트용)
     */
    public static Reference createReferenceWithId(Long id, User user) {
        return new Reference(
                id,
                user,
                TITLE,
                DESCRIPTION,
                IS_PUBLIC,
                COLOR_CODE,
                IS_DEFAULT,
                null
        );
    }

    /**
     * 커스텀 제목과 공개 여부로 Reference 생성
     */
    public static Reference createReferenceWithTitleAndPublic(User user, String title, Boolean isPublic) {
        return Reference.builder()
                .user(user)
                .title(title)
                .description(DESCRIPTION)
                .isPublic(isPublic)
                .colorCode(COLOR_CODE)
                .isDefault(false)
                .build();
    }

    /**
     * 커스텀 제목으로 Reference 생성
     */
    public static Reference createReferenceWithTitle(User user, String title) {
        return Reference.builder()
                .user(user)
                .title(title)
                .description(DESCRIPTION)
                .isPublic(IS_PUBLIC)
                .colorCode(COLOR_CODE)
                .isDefault(false)
                .build();
    }
}