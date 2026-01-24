package swyp12.team9.server.api.link;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import swyp12.team9.server.api.link.dto.LinkResponse;
import swyp12.team9.server.api.link.dto.SaveLinkRequest;
import swyp12.team9.server.global.annotation.CurrentUserId;

@Tag(name = "Link", description = "링크 관리 API")
@RequestMapping("/api/v1/links")
public interface LinkApi {

  @Operation(summary = "링크 저장", description = "특정 폴더(Reference)에 링크를 저장합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "링크 저장 성공", content = @Content(schema = @Schema(implementation = LinkResponse.class))),
      @ApiResponse(responseCode = "400", description = "잘못된 요청"),
      @ApiResponse(responseCode = "404", description = "사용자 또는 폴더를 찾을 수 없음")
  })
  @PostMapping
  swyp12.team9.server.global.common.dto.ApiResponse<LinkResponse> saveLink(
      @Valid @RequestBody SaveLinkRequest request,
      @CurrentUserId Long userId);
}
