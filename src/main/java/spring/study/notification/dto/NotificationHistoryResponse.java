package spring.study.notification.dto;

import java.time.LocalDateTime;
import java.util.List;

public record NotificationHistoryResponse(long result, List<NotificationRealtimeEvent> list, boolean hasMore,
                                          LocalDateTime asOf, LocalDateTime nextBeforeTime, Long nextBeforeId,
                                          List<NotificationGroupCount> unreadCounts) {
}
