package swyp12.team9.server.domain.recommendation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.link.repository.LinkRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Link 데이터를 Elasticsearch에 색인하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LinkIndexingService {

    private final LinkRepository linkRepository;
    private final VectorStore vectorStore;

    /**
     * 모든 링크를 Elasticsearch에 색인
     */
    @Transactional(readOnly = true)
    public void indexAllLinks() {
        List<Link> links = linkRepository.findAll();
        
        if (links.isEmpty()) {
            log.info("색인할 링크가 없습니다.");
            return;
        }

        List<Document> documents = links.stream()
                .map(this::createDocument)
                .collect(Collectors.toList());

        vectorStore.add(documents);
        log.info("총 {} 개의 링크를 Elasticsearch에 색인 완료", documents.size());
    }

    /**
     * 단일 링크를 Elasticsearch에 색인
     */
    @Transactional(readOnly = true)
    public void indexLink(Long linkId) {
        Link link = linkRepository.findById(linkId).orElse(null);
        
        if (link == null) {
            log.warn("링크를 찾을 수 없습니다. ID: {}", linkId);
            return;
        }

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
