package swyp12.team9.server.domain.recommendation.dto;

public record SimilarContentResponse(
    RecommendationResponse content,
    double score) {
}
