package swyp12.team9.server.domain.userlink.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 요일별 개수 Projection
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DayCountProjection {
    private Integer dayOfWeek;
    private Long count;
}