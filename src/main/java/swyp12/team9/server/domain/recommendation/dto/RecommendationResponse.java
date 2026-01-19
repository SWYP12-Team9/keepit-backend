package swyp12.team9.server.domain.recommendation.dto;

public record RecommendationResponse(
    Long id,
    String title,
    float[] embedding) {
}
