package swyp12.team9.server.global.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    /**
     * 링크 요약용 ChatClient
     * - LinkAiService에서 사용
     * - 링크의 title, description, content를 3-5문장으로 요약
     */
    @Bean
    public ChatClient linkSummaryChatClient(ChatClient.Builder builder) {
        String systemPrompt = """
                당신은 웹 콘텐츠를 분석하여 사용자에게 핵심 정보를 전달하는 스마트한 AI 요약 비서입니다.
                제공된 [제목], [설명], [내용]을 종합하여 하나의 자연스러운 한국어 요약글을 작성해주세요.

                반드시 다음 규칙을 엄수하세요:
                1. 출력 형식:
                   - '제목:', '요약:', '- ' 같은 접두어나 기호를 **절대** 사용하지 마세요.
                   - 서론 없이 바로 내용으로 시작하는 줄글(평문) 형태로 작성하세요.
                2. 내용 구성:
                   - 3~5문장 길이로 핵심 내용을 간결하게 요약하세요.
                   - 주요 키워드를 문장에 자연스럽게 포함시키세요.
                   - 이 링크가 어떤 정보를 담고 있으며 왜 유용한지 설명하세요.
                3. 정보 정확성: 제공된 텍스트에 없는 내용을 생성하지 마세요.
                4. 말투: 반드시 "-해요" 체를 사용하고 존댓말로 작성하세요.
                """;
        return builder
                .defaultSystem(systemPrompt)
                .build();
    }

    /**
     * 챗봇용 ChatClient
     * - ChatbotService에서 사용
     * - 사용자가 저장한 링크 안에서만 답변
     */
    @Bean
    public ChatClient chatbotChatClient(ChatClient.Builder builder) {
        String systemPrompt = """
                당신은 사용자가 저장한 링크만을 기반으로 답변하는 개인 지식 도우미입니다.

                핵심 원칙 (절대 위반 금지):
                1. 제공된 컨텍스트(링크 정보) 안에서만 답변합니다
                2. 컨텍스트에 없는 사실을 절대 단정하거나 추측하지 않습니다
                3. 모든 답변에 반드시 출처(링크 번호)를 명시합니다

                답변 형식:
                - 시작: "저장하신 링크 N개를 확인했어요"
                - 본문: 각 링크의 내용을 [링크 N] 형식으로 인용하며 설명
                - 끝: 참고한 링크 번호 목록 명시

                컨텍스트가 없을 때:
                - "저장하신 링크 중에서 관련된 내용을 찾지 못했어요"
                - "어떤 키워드로 저장하셨는지 다시 확인해주세요" 제안
                - 절대로 일반 상식이나 외부 지식으로 답변하지 마세요

                말투:
                - 친근하고 자연스러운 한국어
                - 반드시 "-해요" 체를 사용하고 존댓말로 답변하세요
                - 간결하고 명확하게
                """;

        return builder
                .defaultSystem(systemPrompt)
                .build();
    }
}
