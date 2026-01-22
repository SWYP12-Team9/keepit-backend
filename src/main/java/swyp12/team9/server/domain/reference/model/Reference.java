package swyp12.team9.server.domain.reference.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.access.AccessDeniedException;
import swyp12.team9.server.domain.reference.exception.ReferenceAccessDeniedException;
import swyp12.team9.server.domain.reference.exception.ReferenceValidationException;
import swyp12.team9.server.domain.user.model.User;
import swyp12.team9.server.global.common.entity.BaseEntity;
import swyp12.team9.server.global.exception.ErrorCode;

@Entity
@Table(name = "reference")
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

    @Builder
    public Reference(User user, String title, String description, Boolean isPublic) {
        this.user = user;
        this.title = title;
        this.description = description;
        this.isPublic = isPublic;
    }

    // Reference 생성
    public static Reference create(User user, String title, String description, Boolean isPublic) {

        validateTitle(title);

        return Reference.builder()
                .user(user)
                .title(title)
                .description(description)
                .isPublic(isPublic != null ? isPublic : true)  // null이면 기본값 true
                .build();
    }

    // Reference 정보 수정
    public void update(String title, String description, Boolean isPublic) {

        validateTitle(title);
        validateDescription(description);

        this.title = title;
        this.description = description;
        this.isPublic = isPublic != null ? isPublic : this.isPublic;
    }

    // 소유자 검증
    public void validateOwner(Long userId) {
        if (!this.user.getId().equals(userId)) {
            throw new ReferenceAccessDeniedException("레퍼런스에 대한 권한이 없습니다.");
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