package spring.study.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Borrow {
    UNBORROW("unborrow"), BORROW("borrow출");
    private String value;
}
