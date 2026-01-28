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
     */
    @Transactional(readOnly = true)
    public void indexAllLinks() {
        // 공개된 UserLink에서 Link만 추출 (중복 제거)
        List<Link> publicLinks = userLinkRepository.findByIsPublicTrue()
                .stream()
                .map(UserLink::getLink)
                .distinct()
                .toList();
        
        if (publicLinks.isEmpty()) {
            log.info("색인할 공개 링크가 없습니다.");
            return;
        }

        List<Document> documents = publicLinks.stream()
                .map(this::createDocument)
                .collect(Collectors.toList());

        vectorStore.add(documents);
        log.info("총 {} 개의 공개 링크를 Elasticsearch에 색인 완료", documents.size());
    }

    /**
     * 단일 링크를 Elasticsearch에 색인
     * - 해당 링크가 1명 이상의 사용자에게 공개 설정되어 있어야 색인됨
     */
    @Transactional(readOnly = true)
    public void indexLink(Long linkId) {
        // 해당 linkId를 가진 공개 UserLink가 있는지 확인
        List<UserLink> publicUserLinks = userLinkRepository.findByIsPublicTrue()
                .stream()
                .filter(ul -> ul.getLink().getId().equals(linkId))
                .toList();
        
        if (publicUserLinks.isEmpty()) {
            log.warn("공개된 링크가 없습니다. Link ID: {}", linkId);
            return;
        }

        Link link = publicUserLinks.get(0).getLink();
        Document document = createDocument(link);
        vectorStore.add(List.of(document));
        log.info("링크 색인 완료 - ID: {}, title: {}", linkId, link.getTitle());
    }

    /**
     * Link를 Document로 변환
     * - 제목과 AI 요약만 합쳐서 텍스트로 만듦
     */
    private Document createDocument(Link link) {
        // 제목 + AI요약을 합쳐서 검색용 텍스트 생성
        StringBuilder contentBuilder = new StringBuilder();
        
        if (link.getTitle() != null) {
            contentBuilder.append(link.getTitle()).append(" ");
        }
        if (link.getAiSummary() != null) {
            contentBuilder.append(link.getAiSummary());
        }

        String content = contentBuilder.toString().trim();
        
        // 제목과 AI 요약이 모두 없으면 URL을 content로 사용 (벡터 추출용)
        if (content.isEmpty()) {
            content = link.getUrl();
        }
        
        // 메타데이터 설정
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("linkId", link.getId());
        metadata.put("url", link.getUrl());
        metadata.put("title", link.getTitle() != null ? link.getTitle() : "");
        metadata.put("aiSummary", link.getAiSummary() != null ? link.getAiSummary() : "");
        metadata.put("thumbnailUrl", link.getPreviewImageUrl() != null ? link.getPreviewImageUrl() : "");

        return new Document(String.valueOf(link.getId()), content, metadata);
    }
}
