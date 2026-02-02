package swyp12.team9.server.api.userlink.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import swyp12.team9.server.domain.reference.model.Reference;
import swyp12.team9.server.domain.userlink.model.LinkStatus;
import swyp12.team9.server.domain.userlink.model.UserLink;

@Builder
@Schema(description = "사용자 링크 응답 객체")
public record UserLinkResponse(

        @Schema(description = "사용자 링크 ID", example = "1")
        Long id,

        @Schema(description = "레퍼런스 정보")
        ReferenceInfo reference,

        @Schema(description = "링크 제목", example = "디자인 패턴 가이드")
        String title,

        @Schema(description = "링크 URL", example = "https://example.com/article")
        String url,

        @Schema(description = "AI 요약", example = "디자인 패턴에 대한 종합 가이드")
        String aiSummary,

        @Schema(description = "읽음 상태", example = "UNREAD")
        LinkStatus status,

        @Schema(description = "저장 이유", example = "좋은 아티클이라서")
        String why,

        @Schema(description = "메모", example = "핵심 내용 정리")
        String memo,

        @Schema(description = "조회수", example = "5")
        Long viewCount
) {

    /**
     * UserLink 엔티티와 Reference 목록으로부터 응답 생성
     *
     * @param userLink  UserLink 엔티티
     * @param reference Reference 엔티티
     */
    public static UserLinkResponse from(UserLink userLink, Reference reference) {

        ReferenceInfo newReference = ReferenceInfo.from(reference);

        return UserLinkResponse.builder()
                .id(userLink.getId())
                .reference(newReference)
                .title(userLink.getLink().getTitle())
                .url(userLink.getLink().getUrl())
                .aiSummary(userLink.getLink().getAiSummary())
                .status(userLink.getStatus())
                .why(userLink.getWhy())
                .memo(userLink.getMemo())
                .viewCount(userLink.getViewCount())
                .build();
    }

    /**
     * UserLink 엔티티로부터 응답 생성 (Reference 없음)
     *
     * @param userLink UserLink 엔티티
     */
    public static UserLinkResponse from(UserLink userLink) {
        return UserLinkResponse.builder()
                .id(userLink.getId())
                .reference(null)
                .title(userLink.getLink().getTitle())
                .url(userLink.getLink().getUrl())
                .aiSummary(userLink.getLink().getAiSummary())
                .status(userLink.getStatus())
                .why(userLink.getWhy())
                .memo(userLink.getMemo())
                .viewCount(userLink.getViewCount())
                .build();
    }
}