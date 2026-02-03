package swyp12.team9.server.domain.link.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ScrapingResponse {

    private Boolean success;

    private String title;

    private String description;

    @JsonProperty("favicon_url")
    private String faviconUrl;

    private String url;

    @JsonProperty("content")
    private String content;
}