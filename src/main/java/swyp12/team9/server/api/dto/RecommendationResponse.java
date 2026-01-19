package swyp12.team9.server.api.dto;

public record RecommendationResponse(
    Long id,
    String title,
    float[] embedding) {
}
