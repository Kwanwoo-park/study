package spring.study.todo.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import spring.study.common.entity.BasetimeEntity;
import spring.study.member.entity.Member;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity(name = "todo")
@Table(name = "todo", indexes = {
        @Index(name = "idx_todo_member_id", columnList = "member_id, id"),
        @Index(name = "idx_todo_member_completed_id", columnList = "member_id, completed, id"),
        @Index(name = "idx_todo_member_completed_at", columnList = "member_id, completed, completed_at, id"),
        @Index(name = "idx_todo_member_due_date", columnList = "member_id, completed, due_date, id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Todo extends BasetimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 255)
    private String content;

    @Column(nullable = false)
    private boolean completed;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Builder
    public Todo(Long id, Member member, String content, boolean completed) {
        this.id = id;
        this.member = member;
        this.content = content;
        changeCompleted(completed);
    }

    public void changeContent(String content) {
        this.content = content;
    }

    public void changeDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public void changeCompleted(boolean completed) {
        if (this.completed == completed) return;

        this.completed = completed;
        this.completedAt = null;
        if (completed) this.completedAt = LocalDateTime.now();
    }
}
