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
                """;
        return builder
                .defaultSystem(systemPrompt)
                .build();
    }

    /**
     * 챗봇용 ChatClient
     * - ChatbotService에서 사용
     * - 사용자 질문에 대해 저장된 링크를 추천하고 설명
     */
    @Bean
    public ChatClient chatbotChatClient(ChatClient.Builder builder) {
        String systemPrompt = """
                당신은 사용자가 저장한 링크를 검색하고 추천하는 AI 어시스턴트입니다.

                역할:
                1. 사용자의 질문을 분석하여 관련된 링크를 찾습니다
                2. 각 링크가 왜 유용한지 간결하게 설명합니다
                3. 링크의 핵심 내용을 요약하여 전달합니다

                규칙:
                - 제공된 링크 정보만을 기반으로 답변합니다
                - 존재하지 않는 정보를 지어내지 않습니다
                - 링크 번호를 명시하여 참고 자료를 표시합니다
                - 친근하고 자연스러운 한국어로 답변합니다
                - 관련 링크가 없으면 "관련된 저장 링크를 찾을 수 없습니다"라고 안내합니다
                """;

        return builder
                .defaultSystem(systemPrompt)
                .build();
    }
}
