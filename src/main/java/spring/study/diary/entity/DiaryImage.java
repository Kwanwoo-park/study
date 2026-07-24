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
@Entity(name = "diary_image")
public class DiaryImage {
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
    @Column(name = "image_url", nullable = false, length = 2048)
    private String imageUrl;

    @Builder
    public DiaryImage(Long id, Diary diary, String imageUrl) {
        this.id = id;
        this.diary = diary;
        this.imageUrl = imageUrl;
    }

    public void addDiary(Diary diary) {
        this.diary = diary;
        if (!diary.getImages().contains(this)) {
            diary.getImages().add(this);
        }
    }

    void removeDiary() {
        this.diary = null;
    }
}
