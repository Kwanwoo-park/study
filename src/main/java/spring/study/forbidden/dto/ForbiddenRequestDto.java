package spring.study.forbidden.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.forbidden.entity.Forbidden;
import spring.study.forbidden.entity.Risk;
import spring.study.forbidden.entity.Status;

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
