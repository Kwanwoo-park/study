package spring.study.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Condition {
    FINE("fine"), BROKEN("broken");
    private String value;
}
