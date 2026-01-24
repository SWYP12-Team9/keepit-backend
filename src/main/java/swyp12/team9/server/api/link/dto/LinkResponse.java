package swyp12.team9.server.api.link.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import swyp12.team9.server.domain.link.model.Link;

@Builder
@Schema(description = "링크 저장 응답 객체")
public record LinkResponse(
    @Schema(description = "링크 아이디", example = "1") Long linkId,

    @Schema(description = "링크 제목", example = "자바 스트림 API") String title,

    @Schema(description = "링크 URL", example = "https://www.example.com") String url,

    @Schema(description = "색인 여부", example = "true") boolean indexed) {
  public static LinkResponse from(Link link) {
    return LinkResponse.builder()
        .linkId(link.getId())
        .title(link.getTitle())
        .url(link.getUrl())
        .indexed(true)
        .build();
  }
}
