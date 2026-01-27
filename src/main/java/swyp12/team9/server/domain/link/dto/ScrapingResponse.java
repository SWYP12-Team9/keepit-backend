package swyp12.team9.server.domain.link.dto;

public record ScrapingResponse(
    String title,
    String description,
    String imageUrl,
    String aiSummary) {
}
