package swyp12.team9.server.domain.reference.model;

import jakarta.persistence.*;
import lombok.*;
import swyp12.team9.server.domain.reference.exception.ReferenceAccessDeniedException;
import swyp12.team9.server.domain.reference.exception.ReferenceValidationException;
import swyp12.team9.server.domain.referenceuserlink.model.ReferenceUserLink;
import swyp12.team9.server.domain.user.model.User;
import swyp12.team9.server.global.common.entity.BaseEntity;
import swyp12.team9.server.global.exception.ErrorCode;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "reference",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_reference_user_title",
                        columnNames = {"user_id", "title"}
                )
        }
)
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reference extends BaseEntity {

    @Id
    @Column(name = "reference_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic;

    @Column(name = "color_code", length = 7)
    private String colorCode;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault;

    @OneToMany(mappedBy = "reference",
            cascade = CascadeType.REMOVE,
            orphanRemoval = true)
    private List<ReferenceUserLink> userLinks = new ArrayList<>();

    @Builder
    public Reference(User user, String title, String description, Boolean isPublic, String colorCode, Boolean isDefault) {
        this.user = user;
        this.title = title;
        this.description = description;
        this.isPublic = isPublic;
        this.colorCode = colorCode;
        this.isDefault = isDefault != null ? isDefault : false;
    }

    public static Reference create(User user, String title, String description, Boolean isPublic, String colorCode) {

        validateTitle(title);
        validateDescription(description);

        return Reference.builder()
                .user(user)
                .title(title)
                .description(description)
                .isPublic(isPublic != null ? isPublic : true)
                .colorCode(colorCode)
                .isDefault(false)
                .build();
    }

    public static Reference createDefault(User user) {
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
     * Reference 정보 수정 (개별 파라미터 사용)
     */
    public void update(String title, String description, Boolean isPublic, String colorCode) {

        // null이면 기존 값 유지
        if (title != null) {
            validateTitle(title);
            this.title = title;
        }

        if (description != null) {
            validateDescription(description);
            this.description = description;
        }

        if (isPublic != null) {
            this.isPublic = isPublic;
        }

        if (colorCode != null) {
            this.colorCode = colorCode;
        }
    }

    /**
     * 소유자 검증
     */
    public void validateOwner(Long userId) {
        if (!this.user.getId().equals(userId)) {
            throw new ReferenceAccessDeniedException();
        }
    }

    /**
     * 삭제 가능 여부 검증 (기본 폴더는 삭제 불가)
     */
    public void validateDeletable() {
        if (Boolean.TRUE.equals(this.isDefault)) {
            throw new ReferenceValidationException(ErrorCode.REFERENCE_DEFAULT_CANNOT_DELETE);
        }
    }

    // 제목 검증
    private static void validateTitle(String title) {

        if (title == null || title.trim().isEmpty()) {
            throw new ReferenceValidationException(ErrorCode.REFERENCE_TITLE_REQUIRED);
        }
        if (title.length() > 200) {
        throw new ReferenceValidationException(ErrorCode.REFERENCE_TITLE_TOO_LONG);
        }
    }

    // 설명 검증
    private static void validateDescription(String description) {
        if (description != null && description.length() > 500) {
            throw new ReferenceValidationException(ErrorCode.REFERENCE_DESCRIPTION_TOO_LONG);
        }
    }
}