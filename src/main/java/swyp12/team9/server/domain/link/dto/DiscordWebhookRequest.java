package swyp12.team9.server.domain.link.dto;

public record DiscordWebhookRequest(String content) {

    public static DiscordWebhookRequest of(String content) {
        return new DiscordWebhookRequest(content);
    }
}
