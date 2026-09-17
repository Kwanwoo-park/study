package spring.study.diary.repository;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spring.study.diary.entity.Diary;
import spring.study.diary.entity.DiaryImage;

import java.util.List;

@Repository
public interface DiaryImageRepository extends JpaRepository<DiaryImage, Long> {
    @Transactional(readOnly = true)
    List<DiaryImage> findByDiaryOrderByIdAsc(Diary diary);

    void deleteByDiary(Diary diary);
}
