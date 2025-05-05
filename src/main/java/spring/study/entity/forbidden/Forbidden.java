package spring.study.entity.forbidden;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
@Entity(name = "forbidden")
public class Forbidden {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(unique = true)
    private String word;

    @NotNull
    private Status status;

    @NotNull
    private Risk risk;

    @Builder
    public Forbidden(Long id, String word, Status status, Risk risk) {
        this.id = id;
        this.word = word;
        this.status = status;
        this.risk = risk;
    }
}
