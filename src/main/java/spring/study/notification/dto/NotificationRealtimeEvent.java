package spring.study.notification.dto;

import spring.study.notification.entity.Notification;

import java.time.LocalDateTime;

public record NotificationRealtimeEvent(
        Long id,
        Long memberId,
        String message,
        String readStatus,
        String notiGroup,
        String url,
        LocalDateTime registerTime,
        LocalDateTime updateTime
) {
    public static NotificationRealtimeEvent from(Notification notification) {
        return new NotificationRealtimeEvent(
                notification.getId(),
                notification.getMember().getId(),
                notification.getMessage(),
                notification.getReadStatus().name(),
                notification.getNotiGroup().name(),
                notification.getUrl(),
                notification.getRegisterTime(),
                notification.getUpdateTime()
        );
    }
}
