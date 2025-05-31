package spring.study.forbidden.dto;

import lombok.Getter;
import lombok.ToString;
import spring.study.forbidden.entity.Forbidden;
import spring.study.forbidden.entity.Risk;
import spring.study.forbidden.entity.Status;

@ToString
@Getter
public class ForbiddenResponseDto {
    private Long id;
    private String word;
    private Risk risk;
    private Status status;

    public ForbiddenResponseDto(Forbidden entity) {
        this.id = entity.getId();
        this.word = entity.getWord();
        this.risk = entity.getRisk();
        this.status = entity.getStatus();
    }
}
