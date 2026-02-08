package swyp12.team9.server.domain.link.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;
import swyp12.team9.server.domain.link.model.LinkCategory;

import java.util.Map;

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
     * @param title   링크 제목
     * @param description 링크 설명
     * @param content 링크 콘텐츠 미리보기
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
                    .content();

            log.info("AI 요약 생성 완료");
            return summary;
        } catch (Exception e) {
            log.error("AI 요약 생성 실패 - title: {}, error: {}", title, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 링크 콘텐츠를 분석하여 가장 적합한 카테고리를 분류합니다.
     *
     * @param title       링크 제목
     * @param description 링크 설명
     * @param content     링크 콘텐츠 미리보기
     * @param aiSummary   AI 요약 텍스트
     * @return 분류된 LinkCategory (실패 시 ETC)
     */
    public LinkCategory classifyCategory(String title, String description, String content, String aiSummary) {
        try {
            if (isContentInsufficient(title, description, content)) {
                log.warn("카테고리 분류할 콘텐츠가 부족합니다.");
                return LinkCategory.ETC;
            }

            String categoryNames = String.join(", ", LinkCategory.getAllDisplayNames());

            String promptText = """
                    다음 웹 링크 정보를 분석하여 가장 적합한 카테고리를 하나만 선택해주세요.

                    제목: {title}
                    설명: {description}
                    내용: {content}
                    AI 요약: {aiSummary}

                    카테고리 목록: {categories}

                    규칙:
                    1. 위 카테고리 목록 중 정확히 하나만 선택
                    2. 카테고리명만 출력 (다른 텍스트 없이)
                    3. 적합한 카테고리가 없으면 "기타" 선택
                    """;

            PromptTemplate promptTemplate = new PromptTemplate(promptText);
            Prompt prompt = promptTemplate.create(Map.of(
                    "title", title != null ? title : "제목 없음",
                    "description", description != null ? description : "설명 없음",
                    "content", content != null ? content : "내용 없음",
                    "aiSummary", aiSummary != null ? aiSummary : "요약 없음",
                    "categories", categoryNames
            ));

            String result = chatClient.prompt(prompt)
                    .call()
                    .content();

            String trimmedResult = result != null ? result.trim() : "";
            log.info("AI 카테고리 분류 결과: {}", trimmedResult);

            return LinkCategory.fromDisplayName(trimmedResult)
                    .orElse(LinkCategory.ETC);
        } catch (Exception e) {
            log.error("AI 카테고리 분류 실패 - title: {}, error: {}", title, e.getMessage(), e);
            return LinkCategory.ETC;
        }
    }

    /**
     * 콘텐츠가 요약하기에 충분한지 확인
     * 제목은 무조건, 설명 or 내용 중 최소 하나는 있어야 함
     */
    private boolean isContentInsufficient(String title, String description, String content) {
        boolean titleEmpty = title == null || title.trim().isEmpty();
        boolean descriptionEmpty = description == null || description.trim().isEmpty();
        boolean contentEmpty = content == null || content.trim().isEmpty();

        return titleEmpty || (descriptionEmpty && contentEmpty);
    }
}