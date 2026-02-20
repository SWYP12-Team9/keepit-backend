package swyp12.team9.server.api.chatbot;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import swyp12.team9.server.api.chatbot.dto.ChatbotQueryRequest;
import swyp12.team9.server.api.chatbot.dto.ChatbotQueryResponse;
import swyp12.team9.server.domain.chatbot.service.ChatbotService;
import swyp12.team9.server.global.annotation.CurrentUserId;
import swyp12.team9.server.global.common.dto.ApiResponse;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/chatbots")
@RequiredArgsConstructor
public class ChatbotController implements ChatbotApi {

    private final ChatbotService chatbotService;

    @Override
    public ApiResponse<ChatbotQueryResponse> sendMessage(
            @CurrentUserId Long userId,
            @Valid @RequestBody ChatbotQueryRequest request) {

        ChatbotQueryResponse response = chatbotService.generateResponse(userId, request.message());
        return ApiResponse.ok(response);
    }
}
