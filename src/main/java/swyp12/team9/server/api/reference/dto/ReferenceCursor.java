package swyp12.team9.server.api.reference.dto;

public record ReferenceCursor(Long id, Long linkCount) {

    public static ReferenceCursor from(String cursor, ReferenceSortType sortBy) {
        if (cursor == null || cursor.isBlank()) return null;

        if (sortBy == ReferenceSortType.LINK_COUNT_DESC || sortBy == ReferenceSortType.LINK_COUNT_ASC) {
            String[] parts = cursor.split(":");
            return new ReferenceCursor(Long.parseLong(parts[0]), Long.parseLong(parts[1]));
        }

        return new ReferenceCursor(Long.parseLong(cursor), null);
    }

    public String encode(ReferenceSortType sortBy) {
        if (sortBy == ReferenceSortType.LINK_COUNT_DESC || sortBy == ReferenceSortType.LINK_COUNT_ASC) {
            return id + ":" + linkCount;
        }
        return String.valueOf(id);
    }
}