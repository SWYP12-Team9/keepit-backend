package swyp12.team9.server.domain.sse.event;

public record LinkFailedSsePayload(
        Long userLinkId,
        Long linkId,
        String status,
        String reason
) {

    public static LinkFailedSsePayload of(Long userLinkId, Long linkId, String reason) {
        return new LinkFailedSsePayload(
                userLinkId,
                linkId,
                "FAILED",
                reason != null ? reason : "링크 처리에 실패했습니다."
        );
    }
}
