package swyp12.team9.server.domain.recommendation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.userlink.model.UserLink;
import swyp12.team9.server.domain.userlink.repository.UserLinkRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Link 데이터를 Elasticsearch에 색인하는 서비스
 * - 공개 설정된 UserLink의 Link만 색인 대상
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LinkIndexingService {

    private final UserLinkRepository userLinkRepository;
    private final VectorStore vectorStore;

    /**
     * 공개된 링크만 Elasticsearch에 색인
     * - user_links 테이블에서 is_public = true인 링크만 대상
     * - 동일 Link를 여러 사용자가 공개한 경우 중복 제거
     * - title과 aiSummary가 둘 다 있는 링크만 색인 (예외처리)
     */
    @Transactional(readOnly = true)
    public void indexAllLinks() {
        // 공개된 UserLink 전체 조회
        List<UserLink> publicUserLinks = userLinkRepository.findByIsPublicTrue();
        
        if (publicUserLinks.isEmpty()) {
            log.info("색인할 공개 UserLink가 없습니다.");
            return;
        }

        // title과 aiSummary가 있는 Link를 가진 UserLink만 필터링
        List<UserLink> validUserLinks = publicUserLinks.stream()
                .filter(ul -> hasValidContent(ul.getLink()))
                .toList();
        
        int skippedCount = publicUserLinks.size() - validUserLinks.size();
        if (skippedCount > 0) {
            log.info("Link의 title 또는 aiSummary가 없어 {} 개의 UserLink 색인 제외", skippedCount);
        }
        
        if (validUserLinks.isEmpty()) {
            log.info("색인 가능한 유효한 UserLink가 없습니다.");
            return;
        }

        List<Document> documents = validUserLinks.stream()
                .map(this::createDocument)
                .collect(Collectors.toList());

        vectorStore.add(documents);
        log.info("총 {} 개의 UserLink를 Elasticsearch에 색인 완료", documents.size());
    }

    /**
     * 특정 링크를 가진 모든 공개 UserLink를 Elasticsearch에 색인
     */
    @Transactional(readOnly = true)
    public void indexLink(Long linkId) {
        List<UserLink> publicUserLinks = userLinkRepository.findByIsPublicTrue()
                .stream()
                .filter(ul -> ul.getLink().getId().equals(linkId))
                .toList();
        
        if (publicUserLinks.isEmpty()) {
            log.warn("공개된 UserLink가 없습니다. Link ID: {}", linkId);
            return;
        }

        List<Document> documents = publicUserLinks.stream()
                .filter(ul -> hasValidContent(ul.getLink()))
                .map(this::createDocument)
                .toList();
        
        if (documents.isEmpty()) {
            log.warn("색인 제외 (Link의 title 또는 aiSummary 없음) - ID: {}", linkId);
            return;
        }

        vectorStore.add(documents);
        log.info("Link ID {} 에 대한 {} 개의 UserLink 색인 완료", linkId, documents.size());
    }

    /**
     * 단일 UserLink를 Elasticsearch에 색인 (why, memo 수정 시 호출용)
     */
    @Transactional(readOnly = true)
    public void indexUserLink(Long userLinkId) {
        userLinkRepository.findById(userLinkId).ifPresent(ul -> {
            if (Boolean.TRUE.equals(ul.getIsPublic()) && hasValidContent(ul.getLink())) {
                Document document = createDocument(ul);
                vectorStore.add(List.of(document));
                log.info("UserLink 색인 완료 - ID: {}", userLinkId);
            } else {
                // 비공개 전환 시 기존 문서 삭제
                vectorStore.delete(List.of("ul-" + userLinkId));
                log.info("UserLink 색인 삭제 (비공개 또는 유효하지 않음) - ID: {}", userLinkId);
            }
        });
    }

    /**
     * Link의 title과 aiSummary 유효성 검증
     */
    private boolean hasValidContent(Link link) {
        return link.getTitle() != null && !link.getTitle().trim().isEmpty()
                && link.getAiSummary() != null && !link.getAiSummary().trim().isEmpty();
    }

    /**
     * UserLink를 Document로 변환
     * - 제목 + AI 요약 결합 (검색 대상)
     * - 메타데이터: userId, linkId, userLinkId, url, title, aiSummary, thumbnailUrl 등
     */
    private Document createDocument(UserLink userLink) {
        Link link = userLink.getLink();
        
        // 검색 대상: 제목 + AI요약 + why + memo 결합
        StringBuilder contentBuilder = new StringBuilder();
        contentBuilder.append(link.getTitle()).append(" ")
                     .append(link.getAiSummary());
        
        if (userLink.getWhy() != null && !userLink.getWhy().trim().isEmpty()) {
            contentBuilder.append(" ").append(userLink.getWhy());
        }
        if (userLink.getMemo() != null && !userLink.getMemo().trim().isEmpty()) {
            contentBuilder.append(" ").append(userLink.getMemo());
        }
        
        String content = contentBuilder.toString();
        
        // 메타데이터 설정
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("userId", userLink.getUser().getId());
        metadata.put("linkId", link.getId());
        metadata.put("userLinkId", userLink.getId());
        metadata.put("url", link.getUrl());
        metadata.put("title", link.getTitle());
        metadata.put("aiSummary", link.getAiSummary());
        metadata.put("why", userLink.getWhy() != null ? userLink.getWhy() : "");
        metadata.put("memo", userLink.getMemo() != null ? userLink.getMemo() : "");
        metadata.put("thumbnailUrl", link.getPreviewImageUrl() != null ? link.getPreviewImageUrl() : "");

        // Document ID는 ul-{user_link_id} 형식으로 저장
        return new Document("ul-" + userLink.getId(), content, metadata);
    }
}
