package spring.study.entity.forbidden;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum Risk {
    HIGH("HIGH"), MIDDLE("MIDDLE"), LOW("LOW");

    private String value;
}
