package swyp12.team9.server.api.dto;

public record SimilarContentResponse(
    RecommendationResponse content,
    double score) {
}
