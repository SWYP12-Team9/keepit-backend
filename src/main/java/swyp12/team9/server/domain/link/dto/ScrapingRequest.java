package swyp12.team9.server.domain.link.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ScrapingRequest(
        @JsonProperty("max_length")
        Integer maxLength,

        @JsonProperty("url")
        String url
) {
    public static ScrapingRequest of(String url, Integer maxLength) {
        return new ScrapingRequest(maxLength, url);
    }
}