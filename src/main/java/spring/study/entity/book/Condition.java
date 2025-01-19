package spring.study.entity.book;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Condition {
    양호("fine"), 파손됨("broken");
    private String value;
}
