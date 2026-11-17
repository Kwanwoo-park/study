package spring.study.notification.entity;

import java.time.LocalDateTime;

public enum NotificationPeriod {
    DAY, WEEK, MONTH, ALL;

    public LocalDateTime since(LocalDateTime asOf) {
        return switch (this) {
            case DAY -> asOf.minusDays(1);
            case WEEK -> asOf.minusWeeks(1);
            case MONTH -> asOf.minusMonths(1);
            case ALL -> null;
        };
    }
}
