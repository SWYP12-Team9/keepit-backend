package swyp12.team9.server.domain.recommendation.service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swyp12.team9.server.api.recommendation.dto.RecommendationResponse;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.link.model.LinkCategory;
import swyp12.team9.server.domain.link.repository.LinkRepository;

/**
 * DB에서 링크 정보를 조회하여 추천 콘텐츠를 제공하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {

    private final LinkRepository linkRepository;

    /**
     * 사용자가 읽은 링크 ID 목록을 제외하고 추천 콘텐츠를 가져옵니다.
     *
     * @param readLinkIds 사용자가 읽은 링크 ID 목록 (제외할 ID)
     * @param size        가져올 추천 콘텐츠 수
     * @return 추천 콘텐츠 목록
     */
    public List<RecommendationResponse> getRecommendations(List<Long> readLinkIds, int size) {
        try {
            PageRequest pageRequest = PageRequest.of(0, size);
            List<Link> links;

            if (readLinkIds == null || readLinkIds.isEmpty()) {
                // 읽은 링크가 없으면 전체에서 최신순 조회
                links = linkRepository.findAllByOrderByIdDesc(pageRequest);
            } else {
                // 읽은 링크 제외하고 조회
                links = linkRepository.findByIdNotInOrderByIdDesc(readLinkIds, pageRequest);
            }

            List<RecommendationResponse> responses = links.stream()
                    .map(RecommendationResponse::from)
                    .collect(Collectors.toList());

            log.info("추천 콘텐츠 {} 개 조회 완료 (DB 기반)", responses.size());
            return responses;

        } catch (Exception e) {
            log.error("추천 콘텐츠 조회 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 카테고리별 추천 콘텐츠를 가져옵니다.
     *
     * @param category 카테고리명 (경제/시사, 뷰티/패션 등)
     * @param size     가져올 추천 콘텐츠 수
     * @return 추천 콘텐츠 목록
     */
    public List<RecommendationResponse> getRecommendationsByCategory(String category, int size) {
        try {
            LinkCategory linkCategory = LinkCategory.fromDisplayName(category);

            if (linkCategory == null) {
                log.warn("잘못된 카테고리명: {}", category);
                return Collections.emptyList();
            }

            PageRequest pageRequest = PageRequest.of(0, size);
            List<Link> links = linkRepository.findByCategoryOrderByIdDesc(linkCategory, pageRequest);

            List<RecommendationResponse> responses = links.stream()
                    .map(RecommendationResponse::from)
                    .collect(Collectors.toList());

            log.info("카테고리 [{}] 추천 콘텐츠 {} 개 조회 완료 (DB 기반)", category, responses.size());
            return responses;

        } catch (Exception e) {
            log.error("카테고리별 추천 콘텐츠 조회 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
