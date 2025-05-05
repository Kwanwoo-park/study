package spring.study.dto.forbidden;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.entity.forbidden.Forbidden;
import spring.study.entity.forbidden.Risk;
import spring.study.entity.forbidden.Status;

@Getter
@Setter
@NoArgsConstructor
public class ForbiddenRequestDto {
    private Long id;
    private String word;
    private Risk risk;
    private Status status;

    @Builder
    public ForbiddenRequestDto(Long id, String word, Risk risk, Status status) {
        this.id = id;
        this.word = word;
        this.risk = risk;
        this.status = status;
    }

    public Forbidden toEntity() {
        return Forbidden.builder()
                .word(word)
                .risk(risk)
                .status(status)
                .build();
    }
}
