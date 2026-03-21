package swyp12.team9.server.domain.link.exception;

import swyp12.team9.server.global.exception.BusinessException;
import swyp12.team9.server.global.exception.ErrorCode;

public class DiscordWebhookNotificationException extends BusinessException {

    public DiscordWebhookNotificationException() {
        super(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
