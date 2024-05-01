package spring.study.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity(name = "board")
public class Board extends BasetimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String content;
    private int readCnt;
    private String registerName;
    private String registerEmail;

    @Builder
    public Board(Long id, String title, String content, int readCnt, String registerName, String registerEmail) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.readCnt = readCnt;
        this.registerName = registerName;
        this.registerEmail = registerEmail;
    }
}
