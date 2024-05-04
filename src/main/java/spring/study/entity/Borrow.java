package spring.study.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Borrow {
    비치중("unborrow"), 대출중("borrow");
    private String value;
}
