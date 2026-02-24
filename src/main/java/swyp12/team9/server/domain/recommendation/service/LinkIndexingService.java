package swyp12.team9.server.domain.recommendation.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.recommendation.model.IndexType;
import swyp12.team9.server.domain.userlink.model.UserLink;
import swyp12.team9.server.domain.userlink.repository.UserLinkRepository;

/**
 * 추천 시스템 전용 Elasticsearch 색인 서비스
 * - 공개 설정된 UserLink를 Elasticsearch에 색인
 * - metadata의 indexType으로 추천 인덱스 구분
 * - 탐색 탭의 추천 기능에서만 사용
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LinkIndexingService {

    private final UserLinkRepository userLinkRepository;
    private final VectorStore vectorStore;

    /**
     * 공개된 링크만 Elasticsearch에 색인
     * - Reference의 is_public = true인 UserLink만 대상 - title과 aiSummary가 둘 다 있는 링크만 색인 (유효성 검증)
     * - 검색 대상: title, aiSummary, why, memo 통합
     */
    @Transactional(readOnly = true)
    public void indexAllLinks() {
        // 1. Reference가 공개 설정된 UserLink 전체 조회
        List<UserLink> publicUserLinks = userLinkRepository.findAllByReferenceIsPublicTrue();

        if (publicUserLinks.isEmpty()) {
            log.info("색인할 공개 UserLink가 없습니다.");
            return;
        }

        // 2. title과 aiSummary가 모두 있는 UserLink만 필터링 (검색 품질 보장)
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

        // 3. Document 객체로 변환하여 Elasticsearch에 벌크 색인
        List<Document> documents = validUserLinks.stream()
                .map(this::createDocument)
                .collect(Collectors.toList());

        vectorStore.add(documents);
        log.info("총 {} 개의 UserLink를 Elasticsearch에 색인 완료", documents.size());
    }

    /**
     * 특정 링크를 가진 모든 공개 UserLink를 Elasticsearch에 색인
     * - 신규 링크 추가 시 또는 링크 정보 수정 시 호출
     *
     * @param linkId 색인할 링크 ID
     */
    @Transactional(readOnly = true)
    public void indexLink(Long linkId) {
        // 1. 해당 링크를 참조하는 모든 공개 UserLink 조회 (Reference 기준)
        List<UserLink> publicUserLinks = userLinkRepository.findAllByReferenceIsPublicTrue()
                .stream()
                .filter(ul -> ul.getLink().getId().equals(linkId))
                .toList();

        if (publicUserLinks.isEmpty()) {
            log.warn("공개된 UserLink가 없습니다. Link ID: {}", linkId);
            return;
        }

        // 2. 유효한 내용이 있는 UserLink만 색인 (title, aiSummary 필수)
        List<Document> documents = publicUserLinks.stream()
                .filter(ul -> hasValidContent(ul.getLink()))
                .map(this::createDocument)
                .toList();

        if (documents.isEmpty()) {
            log.warn("색인 제외 (Link의 title 또는 aiSummary 없음) - ID: {}", linkId);
            return;
        }

        // 3. Elasticsearch에 색인 (upsert 동작)
        vectorStore.add(documents);
        log.info("Link ID {} 에 대한 {} 개의 UserLink 색인 완료", linkId, documents.size());
    }

    /**
     * 단일 UserLink를 Elasticsearch에 색인 또는 삭제
     * - UserLink의 why, memo 수정 시 호출 - Reference의 공개 상태 변경 시 색인 추가/삭제 처리
     *
     * @param userLinkId 색인/삭제할 UserLink ID
     */
    @Transactional(readOnly = true)
    public void indexUserLink(Long userLinkId) {
        userLinkRepository.findById(userLinkId).ifPresent(ul -> {
            // Reference가 공개 상태이고 유효한 내용이 있으면 인덱싱(추가/업데이트)
            boolean isPublic = userLinkRepository.isUserLinkInPublicReference(userLinkId);
            if (isPublic && hasValidContent(ul.getLink())) {
                Document document = createDocument(ul);
                vectorStore.add(List.of(document));
                log.info("UserLink 색인 완료 - ID: {}", userLinkId);
            } else {
                // Reference가 비공개이거나 유효하지 않으면 Elasticsearch에서 삭제하여 검색 결과에서 제외
                vectorStore.delete(List.of("recommendation-" + userLinkId));
                log.info("UserLink 색인 삭제 (Reference 비공개 또는 유효하지 않음) - ID: {}", userLinkId);
            }
        });
    }

    /**
     * Link의 title과 aiSummary 유효성 검증
     * - 두 필드 모두 필수 (검색 품질 보장)
     *
     * @param link 검증할 Link 엔티티
     * @return 유효 여부
     */
    private boolean hasValidContent(Link link) {
        return link.getTitle() != null && !link.getTitle().trim().isEmpty()
                && link.getAiSummary() != null && !link.getAiSummary().trim().isEmpty();
    }

    /**
     * UserLink를 Elasticsearch Document로 변환
     * - 검색 대상(content): title + aiSummary + why + memo 통합
     * - 메타데이터: userId, linkId, userLinkId, indexType 등 (응답 생성 시 사용)
     * - Document ID: "recommendation-{userLinkId}" 형식 (챗봇/추천 인덱스 구분)
     *
     * @param userLink 변환할 UserLink 엔티티
     * @return Elasticsearch Document 객체
     */
    private Document createDocument(UserLink userLink) {
        Link link = userLink.getLink();

        // 1. 검색 대상 텍스트 구성
        // - Link의 공통 정보(title, aiSummary)와 사용자별 정보(why, memo)를 통합
        // - 벡터 임베딩 시 이 텍스트가 OpenAI API로 전송됨
        StringBuilder contentBuilder = new StringBuilder();
        contentBuilder.append(link.getTitle()).append(" ")
                .append(link.getAiSummary());

        // 사용자가 작성한 why(이유) 포함
        if (userLink.getWhy() != null && !userLink.getWhy().trim().isEmpty()) {
            contentBuilder.append(" ").append(userLink.getWhy());
        }
        // 사용자가 작성한 memo(메모) 포함
        if (userLink.getMemo() != null && !userLink.getMemo().trim().isEmpty()) {
            contentBuilder.append(" ").append(userLink.getMemo());
        }

        String content = contentBuilder.toString();

        // 2. 메타데이터 설정 (검색 결과에서 응답 생성 시 사용)
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("indexType", IndexType.RECOMMENDATION.getValue());
        metadata.put("userId", userLink.getUser().getId());
        metadata.put("linkId", link.getId());
        metadata.put("userLinkId", userLink.getId());
        metadata.put("url", link.getUrl());
        metadata.put("title", link.getTitle());
        metadata.put("aiSummary", link.getAiSummary());
        metadata.put("why", userLink.getWhy() != null ? userLink.getWhy() : "");
        metadata.put("memo", userLink.getMemo() != null ? userLink.getMemo() : "");
        metadata.put("faviconUrl", link.getFaviconUrl() != null ? link.getFaviconUrl() : "");

        // 3. Document 생성 (ID는 "recommendation-{userLinkId}" 형식)
        // - 동일 ID로 재색인 시 upsert(업데이트) 동작
        return new Document("recommendation-" + userLink.getId(), content, metadata);
    }
}
