package swyp12.team9.server.domain.link.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import swyp12.team9.server.domain.userlink.model.UserLink;
import swyp12.team9.server.domain.userlink.repository.UserLinkRepository;
import swyp12.team9.server.domain.scraper.service.ScraperService;
import swyp12.team9.server.domain.scraper.dto.ScrapedContent;

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
  private final ScraperService scraperService;

  /**
   * 폴더에 링크 저장
   * 
   * @param userId      사용자 ID
   * @param referenceId 폴더(Reference) ID
   * @param url         링크 URL
   * @param purpose     저장 목적
   * @param why         저장 이유
   * @param memo        메모
   * @return 저장된 Link 엔티티
   */
  @Transactional
  public Link saveLink(Long userId, Long referenceId, String url, String purpose, String why, String memo) {

    // 1. 사용자 조회
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));

    // 2. 폴더(Reference) 조회
    Reference reference = referenceRepository.findById(referenceId)
        .orElseThrow(() -> new ReferenceNotFoundException("폴더를 찾을 수 없습니다. ID: " + referenceId));

    // 3. Link 조회 또는 생성 (URL 중복 방지)
    Link link = linkRepository.findByUrl(url)
        .orElseGet(() -> {
          // 웹 스크래핑으로 메타데이터 추출
          ScrapedContent scraped = scraperService.scrapeUrl(url);

          Link newLink = Link.builder()
              .url(url)
              .title(scraped.title() != null && !scraped.title().isBlank()
                  ? scraped.title()
                  : "제목 없음")
              .description(scraped.description() != null && !scraped.description().isBlank()
                  ? scraped.description()
                  : "설명 없음")
              .previewImageUrl(scraped.imageUrl())
              .build();
          Link savedLink = linkRepository.save(newLink);
          log.info("새 링크 생성 - linkId: {}, url: {}, title: {}",
              savedLink.getId(), url, savedLink.getTitle());

          // Elasticsearch에 색인
          indexLinkToEs(savedLink);

          return savedLink;
        });

    // 제목이 없는 경우 재스크래핑 시도 (테스트 및 품질 개선용)
    if ("제목 없음".equals(link.getTitle())) {
      log.info("기존 링크에 제목이 없어 재스크래핑 시도 - linkId: {}", link.getId());
      ScrapedContent scraped = scraperService.scrapeUrl(url);
      if (!"제목 없음".equals(scraped.title())) {
        link.setTitle(scraped.title());
        link.setDescription(scraped.description());
        link.setPreviewImageUrl(scraped.imageUrl());
        linkRepository.save(link);
        indexLinkToEs(link);
      }
    }

    // 4. UserLink 생성 (사용자-링크 관계)
    if (userLinkRepository.existsByUserIdAndLinkId(userId, link.getId())) {
      log.warn("이미 저장된 링크입니다 - userId: {}, linkId: {}", userId, link.getId());
      // 이미 저장된 경우, 기존 UserLink 조회
      UserLink existingUserLink = userLinkRepository.findByUserIdAndLinkId(userId, link.getId())
          .orElseThrow(() -> new IllegalStateException("UserLink 조회 실패"));

      // 5. ReferenceUserLink 생성 (폴더-링크 관계)
      if (!referenceUserLinkRepository.existsByReferenceIdAndUserLinkId(referenceId, existingUserLink.getId())) {
        ReferenceUserLink referenceUserLink = ReferenceUserLink.builder()
            .reference(reference)
            .userLink(existingUserLink)
            .build();
        referenceUserLinkRepository.save(referenceUserLink);
        log.info("폴더에 링크 추가 - referenceId: {}, userLinkId: {}", referenceId, existingUserLink.getId());
      } else {
        log.warn("이미 폴더에 추가된 링크입니다 - referenceId: {}, userLinkId: {}", referenceId, existingUserLink.getId());
      }

      return link;
    }

    UserLink userLink = UserLink.builder()
        .user(user)
        .link(link)
        .purpose(purpose)
        .why(why)
        .isPublic(true) // 기본값: 공개
        .memo(memo)
        .build();
    UserLink savedUserLink = userLinkRepository.save(userLink);
    log.info("UserLink 생성 - userLinkId: {}, userId: {}, linkId: {}",
        savedUserLink.getId(), userId, link.getId());

    // 5. ReferenceUserLink 생성 (폴더-링크 관계)
    ReferenceUserLink referenceUserLink = ReferenceUserLink.builder()
        .reference(reference)
        .userLink(savedUserLink)
        .build();
    referenceUserLinkRepository.save(referenceUserLink);
    log.info("ReferenceUserLink 생성 - referenceId: {}, userLinkId: {}",
        referenceId, savedUserLink.getId());

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
}
