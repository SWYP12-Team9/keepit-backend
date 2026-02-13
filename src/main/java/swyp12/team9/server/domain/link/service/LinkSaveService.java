package swyp12.team9.server.domain.link.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import swyp12.team9.server.domain.link.dto.ScrapingResponse;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.link.repository.LinkRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkSaveService {

    private final LinkRepository linkRepository;

    /**
     * REQUIRES_NEW 트랜잭션에서 Link를 조회하거나 저장합니다.
     * 독립 트랜잭션이므로 커밋 후 다른 스레드에서도 즉시 조회 가능합니다.
     * find를 한 번 더 수행하여 REPEATABLE READ 스냅샷 문제를 방지합니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Link findOrSaveLink(String url, ScrapingResponse data, String aiSummary) {
        String urlHash = Link.generateUrlHash(url);
        return linkRepository.findByUrlHash(urlHash)
                .orElseGet(() -> {
                    Link link = Link.create(
                            url,
                            data.getTitle(),
                            data.getDescription(),
                            data.getFaviconUrl(),
                            data.getContent()
                    );

                    if (aiSummary != null && !aiSummary.trim().isEmpty()) {
                        link.updateAiSummary(aiSummary);
                    }

                    Link saved = linkRepository.save(link);
                    log.info("Link 저장 완료 - linkId: {}, title: {}", saved.getId(), data.getTitle());
                    return saved;
                });
    }
}
