package spring.study.notification.dto;

import spring.study.notification.entity.Group;

public record NotificationGroupCount(Group group, long count) {
}
