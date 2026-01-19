package swyp12.team9.server.api.recommendation.dto;

public record SimilarContentResponse(
    RecommendationResponse content,
    double score) {
}
