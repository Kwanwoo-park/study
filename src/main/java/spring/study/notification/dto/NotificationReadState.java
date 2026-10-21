package spring.study.notification.dto;

import java.time.LocalDateTime;
import java.util.Objects;

/** A committed read watermark, scoped to one notification, one target, or the member. */
public record NotificationReadState(Long memberId, Long notificationId, String notiGroup,
                                    String url, LocalDateTime readBefore) {
    public NotificationReadState {
        Objects.requireNonNull(memberId, "memberId");
        Objects.requireNonNull(readBefore, "readBefore");
        if (notificationId == null && notiGroup != null && url == null) {
            throw new IllegalArgumentException("A target-scoped read requires a URL");
        }
    }

    public boolean matches(NotificationRealtimeEvent event) {
        if (!memberId.equals(event.memberId())) return false;
        if (notificationId != null && !notificationId.equals(event.id())) return false;
        if (notiGroup != null && (!notiGroup.equals(event.notiGroup()) || !Objects.equals(url, event.url()))) return false;
        LocalDateTime revision = event.revisionTime();
        return revision == null || !revision.isAfter(readBefore);
    }

    public String scopeKey() {
        return memberId + ":" + (notificationId != null ? "id:" + notificationId
                : notiGroup != null ? "target:" + notiGroup + ":" + url : "all");
    }
}
