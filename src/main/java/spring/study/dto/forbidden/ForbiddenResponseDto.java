package spring.study.dto.forbidden;

import lombok.Getter;
import lombok.ToString;
import spring.study.entity.forbidden.Forbidden;
import spring.study.entity.forbidden.Risk;
import spring.study.entity.forbidden.Status;

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
