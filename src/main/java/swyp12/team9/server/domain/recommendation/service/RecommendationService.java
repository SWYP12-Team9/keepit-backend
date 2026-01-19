package swyp12.team9.server.domain.recommendation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import swyp12.team9.server.domain.recommendation.dto.RecommendationResponse;
import swyp12.team9.server.domain.recommendation.dto.SimilarContentResponse;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

  private final VectorStore vectorStore;
  private final EmbeddingModel embeddingModel;

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

            return new SimilarContentResponse(new RecommendationResponse(id, title, null), 1.0);
          })
          .collect(Collectors.toList());
    } catch (Exception e) {
      throw new RuntimeException("Recommend failed: " + e.getMessage(), e);
    }
  }
}
