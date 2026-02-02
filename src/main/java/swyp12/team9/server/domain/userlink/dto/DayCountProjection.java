package swyp12.team9.server.domain.userlink.dto;

/**
 * 요일별 개수 Projection
 */
public interface DayCountProjection {
    Integer getDayOfWeek();
    Long getCount();
}