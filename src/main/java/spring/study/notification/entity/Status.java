package spring.study.notification.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Status {
    READ("READ_STATUS"), UNREAD("UNREAD_STATUS");
    private String value;
}
