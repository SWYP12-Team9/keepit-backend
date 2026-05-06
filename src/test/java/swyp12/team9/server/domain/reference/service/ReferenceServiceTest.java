package swyp12.team9.server.domain.reference.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import swyp12.team9.server.domain.reference.dto.request.ReferenceUpdateRequest;
import swyp12.team9.server.domain.reference.event.ReferenceVisibilityChangedEvent;
import swyp12.team9.server.domain.reference.model.Reference;
import swyp12.team9.server.domain.reference.repository.ReferenceRepository;
import swyp12.team9.server.domain.user.model.User;
import swyp12.team9.server.domain.user.model.UserRole;
import swyp12.team9.server.domain.user.model.UserStatus;
import swyp12.team9.server.domain.user.repository.UserRepository;
import swyp12.team9.server.domain.userlink.repository.UserLinkRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReferenceService 단위 테스트")
class ReferenceServiceTest {

    @Mock
    private ReferenceRepository referenceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserLinkRepository userLinkRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ReferenceService referenceService;

    @Nested
    @DisplayName("updateReference 테스트")
    class UpdateReference {

        @Test
        @DisplayName("성공: 공개 여부가 변경되면 visibility 변경 이벤트를 발행한다")
        void success_publishVisibilityChangedEvent() {
            // given
            Long userId = 1L;
            Long referenceId = 10L;
            Reference reference = createReference(referenceId, userId, false);
            ReferenceUpdateRequest request = new ReferenceUpdateRequest("새 제목", "설명", true, "#FFFFFF");

            given(referenceRepository.findById(referenceId)).willReturn(java.util.Optional.of(reference));

            // when
            referenceService.updateReference(userId, referenceId, request);

            // then
            verify(eventPublisher).publishEvent(ReferenceVisibilityChangedEvent.of(referenceId, true));
        }

        @Test
        @DisplayName("성공: 공개 여부가 그대로면 visibility 변경 이벤트를 발행하지 않는다")
        void success_doNotPublishWhenVisibilityUnchanged() {
            // given
            Long userId = 1L;
            Long referenceId = 10L;
            Reference reference = createReference(referenceId, userId, true);
            ReferenceUpdateRequest request = new ReferenceUpdateRequest("새 제목", "설명", true, "#FFFFFF");

            given(referenceRepository.findById(referenceId)).willReturn(java.util.Optional.of(reference));

            // when
            referenceService.updateReference(userId, referenceId, request);

            // then
            verify(eventPublisher, never()).publishEvent(any(ReferenceVisibilityChangedEvent.class));
        }
    }

    private Reference createReference(Long referenceId, Long userId, boolean isPublic) {
        User user = User.builder()
                .username("tester")
                .password("password")
                .isLock(false)
                .isSocial(false)
                .roleType(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(user, "id", userId);

        Reference reference = Reference.create(user, "기존 제목", "기존 설명", isPublic, "#000000");
        ReflectionTestUtils.setField(reference, "id", referenceId);
        return reference;
    }
}
