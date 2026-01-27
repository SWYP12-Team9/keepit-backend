package swyp12.team9.server.api.link;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import swyp12.team9.server.api.link.dto.LinkDetailResponse;
import swyp12.team9.server.api.link.dto.LinkResponse;
import swyp12.team9.server.api.link.dto.SaveLinkRequest;
import swyp12.team9.server.api.link.dto.UpdateLinkRequest;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.link.service.LinkService;
import swyp12.team9.server.domain.userlink.model.LinkStatus;
import swyp12.team9.server.domain.userlink.model.UserLink;
import swyp12.team9.server.global.common.dto.ApiResponse;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
public class LinkController implements LinkApi {

  private final LinkService linkService;

  @Override
  public ApiResponse<LinkResponse> saveLink(SaveLinkRequest request, Long userId) {
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

  @Override
  public ApiResponse<LinkDetailResponse> getLink(Long userLinkId, Long userId) {
    UserLink userLink = linkService.getLink(userId, userLinkId);
    return ApiResponse.ok(LinkDetailResponse.from(userLink));
  }

  @Override
  public ApiResponse<List<LinkDetailResponse>> getLinks(String category, LinkStatus status, Long userId) {
    List<UserLink> userLinks = linkService.getLinks(userId, category, status);
    List<LinkDetailResponse> responses = userLinks.stream()
        .map(LinkDetailResponse::from)
        .collect(Collectors.toList());
    return ApiResponse.ok(responses);
  }

  @Override
  public ApiResponse<LinkDetailResponse> updateLink(Long userLinkId, UpdateLinkRequest request, Long userId) {
    UserLink updatedUserLink = linkService.updateLink(userId, userLinkId, request);
    return ApiResponse.ok(LinkDetailResponse.from(updatedUserLink));
  }

  @Override
  public ApiResponse<Void> deleteLink(Long userLinkId, Long userId) {
    linkService.deleteLink(userId, userLinkId);
    return ApiResponse.ok(null, "링크가 삭제되었습니다.");
  }

  @Override
  public ApiResponse<Void> markAsRead(Long userLinkId, Long userId) {
    linkService.markAsRead(userId, userLinkId);
    return ApiResponse.ok(null, "조회수가 증가하고 읽음 처리되었습니다.");
  }
}
