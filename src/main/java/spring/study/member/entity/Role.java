package spring.study.member.entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public enum Role {
    USER("ROLE_USER"), ADMIN("ROLE_ADMIN"), DENIED("ROLE_DENIED");
    private String value;
}
