package swyp12.team9.server.global.common.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 페이징 응답 DTO
 * Spring Data JPA의 Page 객체를 클라이언트 친화적인 형태로 변환
 */
@Getter
@Builder(access = AccessLevel.PRIVATE)
public class PageResponse<T> {

    private final List<T> content;           // 현재 페이지의 데이터 목록
    private final int page;                  // 현재 페이지 번호 (0부터 시작)
    private final int size;                  // 페이지 크기
    private final long totalElements;        // 전체 요소 개수
    private final int totalPages;            // 전체 페이지 개수
    private final boolean first;             // 첫 페이지 여부
    private final boolean last;              // 마지막 페이지 여부
    private final boolean hasNext;           // 다음 페이지 존재 여부
    private final boolean hasPrevious;       // 이전 페이지 존재 여부

    /**
     * Spring Data JPA의 Page 객체로부터 PageResponse 생성
     */
    public static <T> PageResponse<T> of(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }

    /**
     * 수동으로 페이징 정보를 생성 (QueryDSL 등에서 사용)
     */
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        boolean isFirst = page == 0;
        boolean isLast = page >= totalPages - 1;
        boolean hasNext = page < totalPages - 1;
        boolean hasPrevious = page > 0;

        return PageResponse.<T>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(isFirst)
                .last(isLast)
                .hasNext(hasNext)
                .hasPrevious(hasPrevious)
                .build();
    }

    /**
     * 빈 페이징 응답 생성
     */
    public static <T> PageResponse<T> empty(int page, int size) {
        return PageResponse.<T>builder()
                .content(List.of())
                .page(page)
                .size(size)
                .totalElements(0)
                .totalPages(0)
                .first(true)
                .last(true)
                .hasNext(false)
                .hasPrevious(false)
                .build();
    }
}