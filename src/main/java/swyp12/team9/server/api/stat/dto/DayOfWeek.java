package swyp12.team9.server.api.stat.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import swyp12.team9.server.domain.stat.exception.InvalidDayOfWeekException;

/**
 * 요일 Enum
 */
@Getter
@RequiredArgsConstructor
public enum DayOfWeek {
    MONDAY("MON", "월", 2),
    TUESDAY("TUE", "화", 3),
    WEDNESDAY("WED", "수", 4),
    THURSDAY("THU", "목", 5),
    FRIDAY("FRI", "금", 6),
    SATURDAY("SAT", "토", 7),
    SUNDAY("SUN", "일", 1);

    private final String code;
    private final String koreanName;
    private final int mysqlDayOfWeek;  // MySQL DAYOFWEEK 값 (1=일요일, 2=월요일, ..., 7=토요일)

    /**
     * MySQL DAYOFWEEK 값으로 DayOfWeek 찾기
     * @param mysqlDayOfWeek MySQL DAYOFWEEK 값 (1-7)
     * @return DayOfWeek
     * @throws InvalidDayOfWeekException 유효하지 않은 MySQL DAYOFWEEK 값인 경우
     */
    public static DayOfWeek fromMysqlDayOfWeek(int mysqlDayOfWeek) {
        for (DayOfWeek day : values()) {
            if (day.mysqlDayOfWeek == mysqlDayOfWeek) {
                return day;
            }
        }
        throw new InvalidDayOfWeekException();
    }

    /**
     * 요일 코드로 DayOfWeek 찾기
     * @param code 요일 코드 (MON, TUE, ...)
     * @return DayOfWeek
     * @throws InvalidDayOfWeekException 유효하지 않은 요일 코드인 경우
     */
    public static DayOfWeek fromCode(String code) {
        for (DayOfWeek day : values()) {
            if (day.code.equals(code)) {
                return day;
            }
        }
        throw new InvalidDayOfWeekException();
    }
}