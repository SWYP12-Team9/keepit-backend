package swyp12.team9.server.domain.referenceuserlink.dto;

/**
 * 레퍼런스 ID, 이름, 링크 개수, 색상 코드 Projection
 */
public interface ReferenceWithCountProjection {
    Long getReferenceId();
    String getReferenceTitle();
    Long getCount();
    String getColorCode();
}
