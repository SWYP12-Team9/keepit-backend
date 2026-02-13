package swyp12.team9.server.domain.link.service;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LinkAiService {

    private final ChatClient chatClient;

    public LinkAiService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * 링크 콘텐츠를 요약합니다.
     *
     * @param title       링크 제목
     * @param description 링크 설명
     * @param content     링크 콘텐츠 미리보기
     * @return AI 요약 텍스트 (실패 시 null)
     */
    public String summarizeLink(String title, String description, String content) {
        try {
            // 콘텐츠 정보가 충분하지 않으면 null 반환
            if (isContentInsufficient(title, description, content)) {
                log.warn("요약할 콘텐츠가 부족합니다.");
                return null;
            }

            // 프롬프트 템플릿 생성
            String promptText = """
                    다음 웹 링크 정보를 한국어로 간단히 요약해주세요.
                    
                    제목: {title}
                    설명: {description}
                    내용: {content}
                    
                    요약은 다음 규칙을 따라주세요:
                    1. 3-5문장으로 핵심 내용만 요약
                    2. 핵심 키워드 포함
                    3. 존재하지 않는 정보를 지어내지 말 것
                    4. 이 링크가 왜 유용한지 설명
                    5. 한국어로 작성
                    """;

            PromptTemplate promptTemplate = new PromptTemplate(promptText);
            Prompt prompt = promptTemplate.create(Map.of(
                    "title", title != null ? title : "제목 없음",
                    "description", description != null ? description : "설명 없음",
                    "content", content != null ? content : "내용 없음"
            ));

            String summary = chatClient.prompt(prompt)
                    .call()
                    .content(); // AI API 응답 대기 (수 초)

            log.info("AI 요약 생성 완료");
            return summary;
        } catch (Exception e) {
            log.error("AI 요약 생성 실패 - title: {}, error: {}", title, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 콘텐츠가 요약하기에 충분한지 확인 제목은 무조건, 설명 or 내용 중 최소 하나는 있어야 함
     */
    private boolean isContentInsufficient(String title, String description, String content) {
        boolean titleEmpty = title == null || title.trim().isEmpty();
        boolean descriptionEmpty = description == null || description.trim().isEmpty();
        boolean contentEmpty = content == null || content.trim().isEmpty();

        return titleEmpty || (descriptionEmpty && contentEmpty);
    }
}