package spring.study.diary.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import spring.study.common.entity.CommonVisibility;
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

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(
            name = "visibility",
            nullable = false,
            length = 20,
            columnDefinition = "varchar(20) default 'PUBLIC'"
    )
    private CommonVisibility visibility = CommonVisibility.PUBLIC;

    @JsonIgnore
    @OrderBy("id ASC")
    @OneToMany(mappedBy = "diary", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DiaryImage> images = new ArrayList<>();

    @JsonIgnore
    @OrderBy("todoOrder ASC, id ASC")
    @OneToMany(mappedBy = "diary", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DiaryTodo> todos = new ArrayList<>();

    @Builder
    public Diary(Long id, Member member, String title, String content, CommonVisibility visibility) {
        this.id = id;
        this.member = member;
        this.title = title;
        this.content = content;
        this.visibility = visibility == null ? CommonVisibility.PUBLIC : visibility;
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

    public void update(String title, String content, CommonVisibility visibility) {
        this.title = title;
        this.content = content;
        if (visibility != null) {
            this.visibility = visibility;
        }
    }
}
