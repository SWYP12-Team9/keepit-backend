package swyp12.team9.server.domain.userlink.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import swyp12.team9.server.domain.reference.model.Reference;
import swyp12.team9.server.domain.userlink.model.LinkStatus;
import swyp12.team9.server.domain.userlink.model.UserLink;

@Builder
@Schema(description = "사용자 링크 목록 응답 객체")
public record UserLinkListResponse(

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

        @Schema(description = "조회수", example = "5")
        Long viewCount
) {

    public static UserLinkListResponse of(UserLink userLink, Reference reference) {

        ReferenceInfo newReference = ReferenceInfo.from(reference);

        return UserLinkListResponse.builder()
                .id(userLink.getId())
                .reference(newReference)
                .title(userLink.getLink().getTitle())
                .url(userLink.getLink().getUrl())
                .aiSummary(userLink.getLink().getAiSummary())
                .status(userLink.getStatus())
                .viewCount(userLink.getViewCount())
                .build();
    }
}