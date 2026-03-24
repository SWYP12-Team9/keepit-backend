package swyp12.team9.server.domain.reference.repository;

import swyp12.team9.server.domain.reference.dto.ReferenceCursor;
import swyp12.team9.server.domain.reference.dto.ReferenceSortType;
import swyp12.team9.server.domain.reference.dto.ReferenceType;
import swyp12.team9.server.domain.reference.dto.response.ReferenceListResponse;

import java.util.List;

public interface ReferenceRepositoryCustom {
    List<ReferenceListResponse> findAllWithLinkCount(Long userId, ReferenceType type, ReferenceSortType sortBy, ReferenceCursor referenceCursor, int size);
}