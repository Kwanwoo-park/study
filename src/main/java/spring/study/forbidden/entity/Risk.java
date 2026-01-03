package spring.study.forbidden.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum Risk {
    HIGH(3), MIDDLE(2), LOW(1);

    private int value;
}
