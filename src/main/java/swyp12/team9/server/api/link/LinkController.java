package swyp12.team9.server.api.link;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import swyp12.team9.server.api.link.dto.LinkResponse;
import swyp12.team9.server.api.link.dto.SaveLinkRequest;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.link.service.LinkService;
import swyp12.team9.server.global.annotation.CurrentUserId;
import swyp12.team9.server.global.common.dto.ApiResponse;

@Slf4j
@RestController
@RequiredArgsConstructor
public class LinkController implements LinkApi {

  private final LinkService linkService;

  @Override
  public ApiResponse<LinkResponse> saveLink(SaveLinkRequest request, @CurrentUserId Long userId) {
    log.info("링크 저장 요청 - userId: {}, referenceId: {}, url: {}",
        userId, request.referenceId(), request.url());

    Link link = linkService.saveLink(
        userId,
        request.referenceId(),
        request.url(),
        request.purpose(),
        request.why(),
        request.memo(),
        request.title(),
        request.description(),
        request.imageUrl());

    LinkResponse response = LinkResponse.from(link);
    return ApiResponse.created(response, "링크가 저장되었습니다.");
  }
}
