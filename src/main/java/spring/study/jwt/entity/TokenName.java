package spring.study.jwt.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TokenName {
    ACCESS("access"),
    REFRESH("refresh");

    private String value;
}
