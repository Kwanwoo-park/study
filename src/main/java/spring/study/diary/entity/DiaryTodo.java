package spring.study.diary.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "diary_todo")
public class DiaryTodo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonIgnore
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "diary_id", nullable = false)
    private Diary diary;

    @NotNull
    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "completed", nullable = false)
    private boolean completed;

    @Column(name = "todo_order", nullable = false)
    private int todoOrder;

    @Builder
    public DiaryTodo(Long id, Diary diary, String content, boolean completed, int todoOrder) {
        this.id = id;
        this.diary = diary;
        this.content = content;
        this.completed = completed;
        this.todoOrder = todoOrder;
    }

    public void addDiary(Diary diary) {
        this.diary = diary;
        if (!diary.getTodos().contains(this)) {
            diary.getTodos().add(this);
        }
    }

    void removeDiary() {
        this.diary = null;
    }

    public void update(String content, boolean completed, int todoOrder) {
        this.content = content;
        this.completed = completed;
        this.todoOrder = todoOrder;
    }
}
