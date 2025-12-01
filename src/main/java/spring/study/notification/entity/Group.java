package spring.study.notification.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Group {
    FAVORITE("FAVORITE"), CHAT("CHAT"), COMMENT("COMMENT"), FOLLOW("FOLLOW"), REPLY("REPLY"), ADMIN("ADMIN");
    private String value;
}
