package swyp12.team9.server.global.util;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import lombok.*;

import java.util.List;

public class PaginationUtils {

    @Schema(description = "오프셋 기반 페이징 모델")
    public static class Offset {

        @Schema(description = "오프셋 기반 요청 정보")
        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class PageRequest {

            @Schema(description = "요청 페이지(0부터 시작)", example = "0")
            private int page = 0;

            @Schema(description = "페이지 크기", example = "20")
            private int size = 20;

            @Schema(description = "검색 키워드", example = "developer")
            private String keyword;

            @Schema(description = "정렬 옵션")
            private SortOption sortOption;

            @Schema(description = "정렬 옵션")
            @Getter
            @Setter
            @NoArgsConstructor
            @AllArgsConstructor
            public static class SortOption {

                @Schema(description = "정렬 기준", example = "전체")
                private String field;

                @Schema(description = "정렬 방향")
                private Direction direction;

                @Schema(description = "정렬 방향")
                public enum Direction {
                    ASC, DESC
                }
            }
        }

        @Schema(description = "오프셋 기반 요청 정보")
        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class PageRequestNoSortOption {

            @Schema(description = "요청 페이지(0부터 시작)", example = "0")
            private int page = 0;

            @Schema(description = "페이지 크기", example = "20")
            private int size = 20;

            @Schema(description = "검색 키워드", example = "developer")
            private String keyword;
        }

        @Schema(description = "오프셋 페이징 응답 정보")
        @Getter
        @NoArgsConstructor(access = AccessLevel.PROTECTED)
        @AllArgsConstructor(access = AccessLevel.PROTECTED)
        public static class PageResponse<T> {

            @Schema(description = "결과 데이터 목록")
            private List<T> contents;

            @Schema(description = "현재 페이지", example = "0")
            private int page;

            @Schema(description = "페이지 크기", example = "20")
            private int size;

            @Schema(description = "전체 데이터 개수", example = "153")
            private long totalElements;

            @Schema(description = "전체 페이지 수", example = "8")
            private int totalPages;

            @Schema(description = "다음 페이지 여부", example = "true")
            private boolean hasNext;

            public static <T> PageResponse<T> of(List<T> contents, int page, int size, long totalElements) {
                int totalPages = (int) Math.ceil((double) totalElements / size);
                boolean hasNext = page + 1 < totalPages;

                return new PageResponse<>(contents, page, size, totalElements, totalPages, hasNext);
            }
        }
    }

    @Schema(description = "커서 기반 페이징 모델")
    public static class Cursor {

        @Schema(description = "커서 기반 페이지 요청 정보")
        @Getter
        @Setter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class PageRequest {

            @Schema(description = "현재 커서(첫 요청 시 null)", example = "1", requiredMode = RequiredMode.NOT_REQUIRED)
            private String cursor;

            @Schema(description = "페이지 크기", example = "20", requiredMode = RequiredMode.REQUIRED)
            private int size;

            @Schema(description = "검색 키워드", example = "java")
            private String keyword;

            @Schema(description = "정렬 옵션")
            private SortOption sortOption;

            @Schema(description = "정렬 옵션")
            @Getter
            @Setter
            @NoArgsConstructor
            @AllArgsConstructor
            public static class SortOption {

                @Schema(description = "정렬 기준", example = "채용중")
                private String field;

                @Schema(description = "정렬 방향")
                private Offset.PageRequest.SortOption.Direction direction;

                @Schema(description = "정렬 방향")
                public enum Direction {
                    ASC, DESC
                }
            }
        }

        @Schema(description = "커서 기반 페이지 응답 정보")
        @Getter
        @NoArgsConstructor(access = AccessLevel.PROTECTED)
        @AllArgsConstructor(access = AccessLevel.PROTECTED)
        public static class PageResponse<T> {

            @Schema(description = "결과 데이터 목록")
            private List<T> contents;

            @Schema(description = "다음 페이지 커서(마지막 페이지면 null)", example = "U2FsdGVkX19QZW5kaW5n")
            private String nextCursor;

            @Schema(description = "다음 페이지 존재 여부", example = "true")
            private boolean hasNext;

            public static <T> PageResponse<T> of(
                    List<T> contents, String nextCursor, boolean hasNext
            ) {
                return new PageResponse<>(contents, nextCursor, hasNext);
            }

            public static <T> PageResponse<T> empty() {
                return new PageResponse<>(null, null, false);

            }

        }
    }
}
