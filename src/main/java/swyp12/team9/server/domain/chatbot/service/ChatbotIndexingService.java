package swyp12.team9.server.domain.chatbot.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.link.model.LinkProcessingStatus;
import swyp12.team9.server.domain.recommendation.model.IndexType;
import swyp12.team9.server.domain.userlink.model.UserLink;
import swyp12.team9.server.domain.userlink.repository.UserLinkRepository;

/**
 * 챗봇용 Link 인덱싱 서비스
 * - 사용자별 모든 UserLink를 Elasticsearch에 색인
 * - why, memo에 가중치 부여
 * - userId로 내 링크만 검색
 * - metadata의 indexType으로 챗봇/추천 인덱스 구분
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotIndexingService {

    private final VectorStore vectorStore;
    private final UserLinkRepository userLinkRepository;

    /**
     * 단일 UserLink를 Elasticsearch에 색인 또는 삭제 (챗봇용)
     * - UserLink 생성/수정 시 호출하여 자동 색인
     *
     * @param userLinkId 색인/삭제할 UserLink ID
     */
    @Transactional(readOnly = true)
    public void indexUserLink(Long userLinkId) {
        userLinkRepository.findById(userLinkId).ifPresent(ul -> {
            // 유효한 내용이 있으면 인덱싱(추가/업데이트)
            if (hasValidContent(ul.getLink())) {
                Document document = createDocument(ul);
                vectorStore.add(List.of(document));
                log.info("[챗봇] UserLink 색인 완료 - ID: {}", userLinkId);
            } else {
                // 유효하지 않으면 Elasticsearch에서 삭제
                vectorStore.delete(List.of("chatbot-" + userLinkId));
                log.info("[챗봇] UserLink 색인 삭제 (유효하지 않음) - ID: {}", userLinkId);
            }
        });
    }

    /**
     * UserLink를 Elasticsearch에서 삭제 (챗봇용)
     * - UserLink 삭제 시 호출하여 자동 색인 제거
     *
     * @param userLinkId 삭제할 UserLink ID
     */
    public void deleteUserLink(Long userLinkId) {
        vectorStore.delete(List.of("chatbot-" + userLinkId));
        log.info("[챗봇] UserLink Elasticsearch에서 삭제 - ID: {}", userLinkId);
    }

    /**
     * Link의 title과 aiSummary 유효성 검증
     */
    private boolean hasValidContent(Link link) {
        return link.getProcessingStatus() == LinkProcessingStatus.READY
                && link.getTitle() != null && !link.getTitle().trim().isEmpty()
                && link.getAiSummary() != null && !link.getAiSummary().trim().isEmpty();
    }

    /**
     * UserLink를 Elasticsearch Document로 변환 (챗봇용)
     * - 검색 대상: why/memo 우선, title/summary 보조
     * - 메타데이터: userId, linkId, userLinkId, indexType 등
     * - Document ID: "chatbot-{userLinkId}" 형식 (챗봇/추천 인덱스 구분)
     */
    private Document createDocument(UserLink userLink) {
        Link link = userLink.getLink();

        // 검색 대상 텍스트 구성 (중요도 순: why > title > aiSummary > memo)
        StringBuilder contentBuilder = new StringBuilder();

        // 1. why
        if (userLink.getWhy() != null && !userLink.getWhy().trim().isEmpty()) {
            contentBuilder.append("저장 이유: ").append(userLink.getWhy()).append(". ");
        }

        // 2. title
        contentBuilder.append(link.getTitle()).append(". ");

        // 3. aiSummary
        contentBuilder.append(link.getAiSummary()).append(". ");

        // 4. memo
        if (userLink.getMemo() != null && !userLink.getMemo().trim().isEmpty()) {
            contentBuilder.append("메모: ").append(userLink.getMemo()).append(".");
        }

        String content = contentBuilder.toString();

        // 메타데이터 설정
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("indexType", IndexType.CHATBOT.getValue());
        metadata.put("userId", userLink.getUser().getId());
        metadata.put("linkId", link.getId());
        metadata.put("userLinkId", userLink.getId());
        metadata.put("url", link.getUrl());
        metadata.put("title", link.getTitle());
        metadata.put("aiSummary", link.getAiSummary());
        metadata.put("why", userLink.getWhy() != null ? userLink.getWhy() : "");
        metadata.put("memo", userLink.getMemo() != null ? userLink.getMemo() : "");
        metadata.put("faviconUrl", link.getFaviconUrl() != null ? link.getFaviconUrl() : "");

        // Document 생성 (ID는 "chatbot-{userLinkId}" 형식)
        return new Document("chatbot-" + userLink.getId(), content, metadata);
    }
}
