package swyp12.team9.server.domain.link.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swyp12.team9.server.api.link.dto.UpdateLinkRequest;
import swyp12.team9.server.domain.link.client.ScraperClient;
import swyp12.team9.server.domain.link.dto.ScrapingResponse;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.link.repository.LinkRepository;
import swyp12.team9.server.domain.recommendation.service.RecommendationService;
import swyp12.team9.server.domain.reference.model.Reference;
import swyp12.team9.server.domain.reference.repository.ReferenceRepository;
import swyp12.team9.server.domain.referenceuserlink.model.ReferenceUserLink;
import swyp12.team9.server.domain.referenceuserlink.repository.ReferenceUserLinkRepository;
import swyp12.team9.server.domain.user.exception.UserNotFoundException;
import swyp12.team9.server.domain.user.model.User;
import swyp12.team9.server.domain.user.repository.UserRepository;
import swyp12.team9.server.domain.reference.exception.ReferenceNotFoundException;
import swyp12.team9.server.domain.userlink.model.LinkStatus;
import swyp12.team9.server.domain.userlink.model.UserLink;
import swyp12.team9.server.domain.userlink.repository.UserLinkRepository;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LinkService {

  private final LinkRepository linkRepository;
  private final UserLinkRepository userLinkRepository;
  private final ReferenceUserLinkRepository referenceUserLinkRepository;
  private final UserRepository userRepository;
  private final ReferenceRepository referenceRepository;
  private final RecommendationService recommendationService;
  private final ScraperClient scraperClient;

  /**
   * 폴더에 링크 저장
   */
  @Transactional
  public Link saveLink(Long userId, Long referenceId, String url, String purpose, String why, String memo,
      String title, String description, String imageUrl) {

    // 1. 사용자 및 폴더 조회
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));

    Reference reference = referenceRepository.findById(referenceId)
        .orElseThrow(() -> new ReferenceNotFoundException("폴더를 찾을 수 없습니다. ID: " + referenceId));

    // 2. Link 조회 또는 생성
    Link link = linkRepository.findByUrl(url)
        .orElseGet(() -> {
          // 새 링크인 경우 스크래핑 시도
          ScrapingResponse scrapingInfo = scraperClient.scrapeUrl(url);

          String finalTitle = (title != null && !title.isBlank()) ? title
              : (scrapingInfo != null ? scrapingInfo.title() : "제목 없음");
          String finalDesc = (description != null && !description.isBlank()) ? description
              : (scrapingInfo != null ? scrapingInfo.description() : "설명 없음");
          String finalImage = (imageUrl != null && !imageUrl.isBlank()) ? imageUrl
              : (scrapingInfo != null ? scrapingInfo.imageUrl() : null);
          String aiSummary = (scrapingInfo != null) ? scrapingInfo.aiSummary() : null;

          Link newLink = Link.builder()
              .url(url)
              .title(finalTitle)
              .description(finalDesc)
              .previewImageUrl(finalImage)
              .build();

          if (aiSummary != null && !aiSummary.isBlank()) {
            newLink.setAiSummary(aiSummary);
          }

          Link savedLink = linkRepository.save(newLink);
          log.info("새 링크 생성 및 스크래핑 완료 - linkId: {}", savedLink.getId());

          indexLinkToEs(savedLink);

          return savedLink;
        });

    // 기존 링크 업데이트 로직 (메타데이터가 부족할 경우)
    if ("제목 없음".equals(link.getTitle()) || link.getAiSummary() == null) {
      boolean updated = false;
      ScrapingResponse scrapingInfo = scraperClient.scrapeUrl(url);

      if (scrapingInfo != null) {
        if ("제목 없음".equals(link.getTitle()) && scrapingInfo.title() != null) {
          link.setTitle(scrapingInfo.title());
          updated = true;
        }
        if ((link.getDescription() == null || link.getDescription().isBlank()) && scrapingInfo.description() != null) {
          link.setDescription(scrapingInfo.description());
          updated = true;
        }
        if (link.getPreviewImageUrl() == null && scrapingInfo.imageUrl() != null) {
          link.setPreviewImageUrl(scrapingInfo.imageUrl());
          updated = true;
        }
        if (link.getAiSummary() == null && scrapingInfo.aiSummary() != null) {
          link.setAiSummary(scrapingInfo.aiSummary());
          updated = true;
        }
      }

      if (updated) {
        linkRepository.save(link);
        indexLinkToEs(link);
      }
    }

    // 4. UserLink 처리
    Optional<UserLink> existingUserLinkOpt = userLinkRepository.findByUserIdAndLinkId(userId, link.getId());

    if (existingUserLinkOpt.isPresent()) {
      UserLink existingUserLink = existingUserLinkOpt.get();
      // 폴더 매핑 추가
      if (!referenceUserLinkRepository.existsByReferenceIdAndUserLinkId(referenceId, existingUserLink.getId())) {
        ReferenceUserLink referenceUserLink = ReferenceUserLink.builder()
            .reference(reference)
            .userLink(existingUserLink)
            .build();
        referenceUserLinkRepository.save(referenceUserLink);
      }
      return link;
    }

    UserLink userLink = UserLink.builder()
        .user(user)
        .link(link)
        .purpose(purpose)
        .why(why)
        .isPublic(true)
        .memo(memo)
        .build();
    UserLink savedUserLink = userLinkRepository.save(userLink);

    ReferenceUserLink referenceUserLink = ReferenceUserLink.builder()
        .reference(reference)
        .userLink(savedUserLink)
        .build();
    referenceUserLinkRepository.save(referenceUserLink);

    return link;
  }

  private void indexLinkToEs(Link link) {
    try {
      recommendationService.indexLink(link);
      log.info("링크 색인 완료 - linkId: {}", link.getId());
    } catch (Exception e) {
      log.error("링크 색인 실패 - linkId: {}, error: {}", link.getId(), e.getMessage());
    }
  }

  public UserLink getLink(Long userId, Long userLinkId) {
    UserLink userLink = userLinkRepository.findById(userLinkId)
        .orElseThrow(() -> new RuntimeException("Link not found"));

    if (!userLink.getUser().getId().equals(userId)) {
      throw new RuntimeException("Unauthorized access");
    }
    return userLink;
  }

  public List<UserLink> getLinks(Long userId, String purpose, LinkStatus status) {
    return userLinkRepository.findLinksByConditions(userId, purpose, status);
  }

  @Transactional
  public UserLink updateLink(Long userId, Long userLinkId, UpdateLinkRequest request) {
    UserLink userLink = getLink(userId, userLinkId);

    String newPurpose = request.purpose() != null ? request.purpose() : userLink.getPurpose();
    Boolean newIsPublic = request.isPublic() != null ? request.isPublic() : userLink.getIsPublic();
    String newMemo = request.memo() != null ? request.memo() : userLink.getMemo();

    userLink.updateUserLink(newPurpose, userLink.getWhy(), newIsPublic, newMemo);

    if (request.status() != null) {
      userLink.changeStatus(request.status());
    }

    return userLink;
  }

  @Transactional
  public void deleteLink(Long userId, Long userLinkId) {
    UserLink userLink = getLink(userId, userLinkId);
    referenceUserLinkRepository.deleteByUserLinkId(userLinkId);
    userLinkRepository.delete(userLink);
  }

  @Transactional
  public void markAsRead(Long userId, Long linkId) {
    UserLink userLink = getLink(userId, linkId);
    userLink.markAsRead();
  }
}
