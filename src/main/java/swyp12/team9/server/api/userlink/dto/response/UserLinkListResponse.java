package swyp12.team9.server.api.userlink.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import swyp12.team9.server.domain.reference.model.Reference;
import swyp12.team9.server.domain.userlink.model.LinkStatus;
import swyp12.team9.server.domain.userlink.model.UserLink;

import java.util.List;
import java.util.stream.Collectors;

@Builder
@Schema(description = "사용자 링크 목록 응답 객체")
public record UserLinkListResponse(

        @Schema(description = "사용자 링크 ID", example = "1")
        Long id,

        @Schema(description = "레퍼런스 정보 목록")
        List<ReferenceInfo> references,

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

    public static UserLinkListResponse of(UserLink userLink, List<Reference> references) {
        List<ReferenceInfo> referenceInfos = references.stream()
                .map(ReferenceInfo::from)
                .collect(Collectors.toList());

        return UserLinkListResponse.builder()
                .id(userLink.getId())
                .references(referenceInfos)
                .title(userLink.getLink().getTitle())
                .url(userLink.getLink().getUrl())
                .aiSummary(userLink.getLink().getAiSummary())
                .status(userLink.getStatus())
                .viewCount(userLink.getViewCount())
                .build();
    }
}