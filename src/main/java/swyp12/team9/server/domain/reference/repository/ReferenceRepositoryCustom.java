package swyp12.team9.server.domain.reference.repository;

import swyp12.team9.server.api.reference.dto.ReferenceSortType;
import swyp12.team9.server.api.reference.dto.ReferenceType;
import swyp12.team9.server.api.reference.dto.response.ReferenceListResponse;

import java.util.List;

public interface ReferenceRepositoryCustom {
    List<ReferenceListResponse> findAllWithLinkCount(Long userId, ReferenceType type, ReferenceSortType sortBy, Long cursorId, int size);

    // 미지정 폴더(Reference is Null)의 링크 개수 조회
    Long countUnspecifiedLinks(Long userId);
}