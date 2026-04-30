package swyp12.team9.server.domain.reference.event;

/**
 * Reference 공개 여부 변경 이벤트.
 * 연결된 UserLink들의 추천 인덱싱을 다시 처리하기 위해 변경된 Reference ID와 최종 공개 상태만 전달한다.
 */
public record ReferenceVisibilityChangedEvent(Long referenceId, Boolean isPublic) {

    public static ReferenceVisibilityChangedEvent of(Long referenceId, Boolean isPublic) {
        return new ReferenceVisibilityChangedEvent(referenceId, isPublic);
    }
}
