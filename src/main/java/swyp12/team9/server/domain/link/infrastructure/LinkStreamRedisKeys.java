package swyp12.team9.server.domain.link.infrastructure;

import lombok.experimental.UtilityClass;

@UtilityClass
public class LinkStreamRedisKeys {

    public static String notifyUsersKey(Long linkId) {
        return "link:notify_users:" + linkId;
    }

    public static String processingLockKey(Long linkId) {
        return "link:processing_lock:" + linkId;
    }

    public static String payloadBackupKey(String recordId) {
        return "link:stream_payload:" + recordId;
    }

    public static String linkIdBackupKey(String recordId) {
        return "link:stream_link_id:" + recordId;
    }
}
