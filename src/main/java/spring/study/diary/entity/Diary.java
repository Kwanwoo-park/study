package spring.study.diary.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import spring.study.common.entity.BasetimeEntity;
import spring.study.member.entity.Member;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity(name = "diary")
public class Diary extends BasetimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonIgnore
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @NotNull
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @NotNull
    @Lob
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @JsonIgnore
    @OrderBy("id ASC")
    @OneToMany(mappedBy = "diary", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DiaryImage> images = new ArrayList<>();

    @JsonIgnore
    @OrderBy("todoOrder ASC, id ASC")
    @OneToMany(mappedBy = "diary", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DiaryTodo> todos = new ArrayList<>();

    @Builder
    public Diary(Long id, Member member, String title, String content) {
        this.id = id;
        this.member = member;
        this.title = title;
        this.content = content;
    }

    public void addMember(Member member) {
        this.member = member;
        if (!member.getDiaries().contains(this)) {
            member.getDiaries().add(this);
        }
    }

    public void addImage(DiaryImage image) {
        if (!images.contains(image)) {
            images.add(image);
        }
        image.addDiary(this);
    }

    public void removeImage(DiaryImage image) {
        if (images.remove(image)) {
            image.removeDiary();
        }
    }

    public void addTodo(DiaryTodo todo) {
        if (!todos.contains(todo)) {
            todos.add(todo);
        }
        todo.addDiary(this);
    }

    public void removeTodo(DiaryTodo todo) {
        if (todos.remove(todo)) {
            todo.removeDiary();
        }
    }

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }
}
