package swyp12.team9.server.domain.recommendation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import swyp12.team9.server.api.recommendation.dto.RecommendationResponse;
import swyp12.team9.server.api.recommendation.dto.SimilarContentResponse;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.link.repository.LinkRepository;
import swyp12.team9.server.domain.reference.exception.ReferenceNotFoundException;
import swyp12.team9.server.domain.reference.model.Reference;
import swyp12.team9.server.domain.reference.repository.ReferenceRepository;
import swyp12.team9.server.domain.referenceuserlink.repository.ReferenceUserLinkRepository;
import swyp12.team9.server.domain.referenceuserlink.model.ReferenceUserLink;
import swyp12.team9.server.domain.userlink.repository.UserLinkRepository;
import swyp12.team9.server.domain.userlink.model.UserLink;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

  private final VectorStore vectorStore;
  private final EmbeddingModel embeddingModel;
  private final ReferenceUserLinkRepository referenceUserLinkRepository;
  private final UserLinkRepository userLinkRepository;
  private final LinkRepository linkRepository;
  private final ReferenceRepository referenceRepository;

  /**
   * 링크를 Elasticsearch에 색인
   * 
   * @param link 색인할 링크
   */
  public void indexLink(Link link) {
    try {
      // 제목 + 본문을 합쳐서 임베딩
      String content = (link.getTitle() != null ? link.getTitle() : "") + " " +
          (link.getDescription() != null ? link.getDescription() : "");

      if (content.trim().isEmpty()) {
        log.warn("링크 내용이 비어있어 색인하지 않습니다 - linkId: {}", link.getId());
        return;
      }

      Document document = new Document(
          "link_" + link.getId(),
          content,
          Map.of(
              "link_id", link.getId(),
              "title", link.getTitle() != null ? link.getTitle() : "",
              "url", link.getUrl()));

      vectorStore.add(List.of(document));
      log.info("링크 색인 완료 - linkId: {}, title: {}", link.getId(), link.getTitle());
    } catch (Exception e) {
      log.error("링크 색인 실패 - linkId: {}, error: {}", link.getId(), e.getMessage(), e);
      throw new RuntimeException("링크 색인 실패: " + e.getMessage(), e);
    }
  }

  /**
   * 폴더 기반 추천
   * 
   * @param userId      사용자 ID
   * @param referenceId 폴더(Reference) ID
   * @param size        추천 개수
   * @return 추천 링크 목록
   */
  public List<SimilarContentResponse> recommendByFolder(Long userId, Long referenceId, int size) {
    try {
      // 1. 폴더 내 링크 개수 확인
      Long linkCount = referenceUserLinkRepository.countByReferenceIdAndUserId(referenceId, userId);

      if (linkCount == 0) {
        // 초기 사용자: 폴더 이름 기반 추천
        log.info("폴더에 링크가 없음 - 초기 사용자 추천 실행 - referenceId: {}", referenceId);
        return recommendForNewUser(referenceId, size);
      }

      // 2. 폴더 내 모든 링크 정보(UserLink 포함) 조회
      List<ReferenceUserLink> referenceUserLinks = referenceUserLinkRepository
          .findAllByReferenceIdAndUserId(referenceId, userId);

      // 링크 ID 목록 추출 (나중에 제거용)
      List<Long> linkIds = referenceUserLinks.stream()
          .map(rul -> rul.getUserLink().getLink().getId())
          .collect(Collectors.toList());

      log.info("폴더 내 링크 개수: {} - referenceId: {}, userId: {}", linkIds.size(), referenceId, userId);

      // 3. 폴더 내 모든 링크의 내용 + 사용자 입력(메모 등)을 합쳐서 검색어 생성
      String combinedContent = referenceUserLinks.stream()
          .map(rul -> {
            UserLink ul = rul.getUserLink();
            Link l = ul.getLink();

            String linkContent = (l.getTitle() != null ? l.getTitle() : "") + " " +
                (l.getDescription() != null ? l.getDescription() : "");

            String userContent = (ul.getPurpose() != null ? ul.getPurpose() : "") + " " +
                (ul.getWhy() != null ? ul.getWhy() : "") + " " +
                (ul.getMemo() != null ? ul.getMemo() : "");

            return linkContent + " " + userContent;
          })
          .collect(Collectors.joining(" "));

      if (combinedContent.trim().length() < 2) {
        log.warn("폴더 내 링크 내용이 부족함. 폴더 제목을 대신 사용합니다. - referenceId: {}", referenceId);
        Reference reference = referenceRepository.findById(referenceId)
            .orElseThrow(() -> new ReferenceNotFoundException("폴더를 찾을 수 없습니다."));
        combinedContent = reference.getTitle();
      }

      // 5. 평균 벡터로 유사도 검색
      List<Document> results = vectorStore.similaritySearch(
          SearchRequest.builder()
              .query(combinedContent)
              .topK(size + linkIds.size() + 5) // 넉넉하게 조회
              .build());

      // 6. 사용자가 이미 저장한 링크 제외
      Set<Long> savedLinkIds = new HashSet<>(userLinkRepository.findLinkIdsByUserId(userId));

      List<SimilarContentResponse> finalResults = results.stream()
          .filter(doc -> {
            try {
              Object idObj = doc.getMetadata().get("link_id");
              Long linkId = (idObj instanceof Number) ? ((Number) idObj).longValue() : 0L;
              return !savedLinkIds.contains(linkId);
            } catch (Exception e) {
              log.error("필터링 중 에러: {}", e.getMessage());
              return false;
            }
          })
          .limit(size)
          .map(doc -> {
            Object idObj = doc.getMetadata().get("link_id");
            Long linkId = (idObj instanceof Number) ? ((Number) idObj).longValue() : 0L;
            String title = (String) doc.getMetadata().getOrDefault("title", "제목 없음");
            return SimilarContentResponse.builder()
                .content(RecommendationResponse.builder()
                    .id(linkId)
                    .title(title)
                    .embedding(null)
                    .build())
                .score(1.0)
                .build();
          })
          .collect(Collectors.toList());

      return finalResults;

    } catch (Exception e) {
      log.error("폴더 기반 추천 실패 - referenceId: {}, userId: {}, error: {}",
          referenceId, userId, e.getMessage(), e);
      throw new RuntimeException("추천 실패: " + e.getMessage(), e);
    }
  }

  /**
   * 초기 사용자 추천 (링크가 없을 때)
   * 
   * @param referenceId 폴더 ID
   * @param size        추천 개수
   * @return 추천 링크 목록
   */
  public List<SimilarContentResponse> recommendForNewUser(Long referenceId, int size) {
    try {
      // 옵션 2: 폴더 이름을 임베딩하여 유사 링크 추천
      Reference reference = referenceRepository.findById(referenceId)
          .orElseThrow(() -> new ReferenceNotFoundException("폴더를 찾을 수 없습니다. ID: " + referenceId));

      String folderName = reference.getTitle();
      log.info("폴더 이름 기반 추천 - folderName: {}", folderName);

      List<Document> results = vectorStore.similaritySearch(
          SearchRequest.builder()
              .query(folderName)
              .topK(size)
              .build());

      return results.stream()
          .map(doc -> {
            Long linkId = (Long) doc.getMetadata().get("link_id");
            String title = (String) doc.getMetadata().getOrDefault("title", "제목 없음");
            return SimilarContentResponse.builder()
                .content(RecommendationResponse.builder()
                    .id(linkId)
                    .title(title)
                    .embedding(null)
                    .build())
                .score(1.0)
                .build();
          })
          .collect(Collectors.toList());

    } catch (Exception e) {
      log.error("초기 사용자 추천 실패 - referenceId: {}, error: {}", referenceId, e.getMessage(), e);
      // 실패 시 빈 목록 반환
      return Collections.emptyList();
    }
  }

  // ========== 테스트용 메서드 (기존 유지) ==========

  public void seedData() {
    try {
      List<Document> documents = List.of(
          new Document("1", "자바 스트림 API 성능 최적화 기법", Map.of("title", "자바 스트림 API 성능 최적화", "category", "Java")),
          new Document("2", "스프링 시큐리티 JWT 설정 및 보안 가이드", Map.of("title", "스프링 시큐리티 JWT 설정하기", "category", "Security")),
          new Document("3", "자바 기반의 웹 보안 취약점 점검", Map.of("title", "자바 기반의 보안 가이드", "category", "Java/Security")),
          new Document("4", "React Context API와 성능 최적화",
              Map.of("title", "React Context API 활용법", "category", "Frontend")),
          new Document("5", "자바 객체지향 설계 원칙 (SOLID)", Map.of("title", "자바의 정석 기초 스터디", "category", "Java")));
      vectorStore.add(documents);
    } catch (Exception e) {
      throw new RuntimeException("Seed failed: " + e.getMessage(), e);
    }
  }

  public float[] calculateUserInterest(List<Long> readDocs) {
    return embeddingModel.embed("자바 개발 및 스프링 보안 전문가");
  }

  public List<SimilarContentResponse> recommend(float[] userInterest) {
    try {
      List<Document> result = vectorStore.similaritySearch(
          SearchRequest.builder()
              .query("자바 개발 전문가")
              .topK(5)
              .build());

      return result.stream()
          .map(doc -> {
            String title = doc.getMetadata() != null ? (String) doc.getMetadata().getOrDefault("title", "Untitled")
                : "No Metadata";
            long id = 0;
            try {
              id = Long.parseLong(doc.getId());
            } catch (NumberFormatException ignored) {
            }

            return SimilarContentResponse.builder()
                .content(RecommendationResponse.builder()
                    .id(id)
                    .title(title)
                    .embedding(null)
                    .build())
                .score(1.0)
                .build();
          })
          .collect(Collectors.toList());
    } catch (Exception e) {
      throw new RuntimeException("Recommend failed: " + e.getMessage(), e);
    }
  }
}
