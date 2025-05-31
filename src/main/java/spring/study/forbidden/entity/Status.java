package spring.study.forbidden.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum Status {
    PROPOSAL("PROPOSAL_STATUS"), EXAMINE("EXAMINE_STATUS"), APPROVAL("APPROVAL_STATUS");
    private String value;
}
