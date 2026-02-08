package swyp12.team9.server.domain.referenceuserlink.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 레퍼런스 카테고리별 개수 Projection
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReferenceCategoryCountProjection {
    private Long referenceId;
    private String referenceTitle;
    private String referenceColorCode;
    private Long count;
}