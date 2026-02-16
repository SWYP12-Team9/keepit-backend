package swyp12.team9.server.api.chatbot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "챗봇 질문 요청 객체")
public record ChatbotQueryRequest(

        @Schema(
                description = "사용자 질문 메시지",
                example = "경제 관련 링크 알려줘",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "질문은 필수입니다.")
        @Size(min = 1, max = 1000, message = "질문은 1자 이상 1000자 이하로 입력해주세요.")
        String message
) {
}
