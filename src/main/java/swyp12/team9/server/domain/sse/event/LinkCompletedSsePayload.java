package swyp12.team9.server.domain.sse.event;

public record LinkCompletedSsePayload(
        Long userLinkId,
        Long linkId,
        String title,
        String status
) {

    public static LinkCompletedSsePayload of(Long userLinkId, Long linkId, String title) {
        return new LinkCompletedSsePayload(
                userLinkId,
                linkId,
                title != null ? title : "제목 없음",
                "COMPLETED"
        );
    }
}
