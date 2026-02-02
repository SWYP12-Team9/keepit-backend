package swyp12.team9.server.domain.referenceuserlink.dto;

/**
 * 레퍼런스 카테고리별 개수 Projection
 */
public interface ReferenceCategoryCountProjection {
    String getReferenceTitle();
    Long getCount();
}