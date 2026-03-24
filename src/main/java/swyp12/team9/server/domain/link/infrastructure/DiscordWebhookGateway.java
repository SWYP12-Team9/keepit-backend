package swyp12.team9.server.domain.link.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import swyp12.team9.server.domain.link.dto.DiscordWebhookRequest;
import swyp12.team9.server.domain.link.exception.DiscordWebhookNotificationException;

@Slf4j
@Component
@RequiredArgsConstructor
public class DiscordWebhookGateway {

    private final RestClient.Builder restClientBuilder;

    @Value("${discord.webhook.url:}")
    private String webhookUrl;

    public void send(String content) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }

        restClientBuilder.build()
                .post()
                .uri(webhookUrl)
                .body(DiscordWebhookRequest.of(content))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                    throw new DiscordWebhookNotificationException();
                })
                .toBodilessEntity();

        log.info("Discord webhook 알림 전송 성공");
    }
}
