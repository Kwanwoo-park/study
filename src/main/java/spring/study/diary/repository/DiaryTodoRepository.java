package spring.study.diary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spring.study.diary.entity.Diary;
import spring.study.diary.entity.DiaryTodo;

import java.util.List;

@Repository
public interface DiaryTodoRepository extends JpaRepository<DiaryTodo, Long> {
    List<DiaryTodo> findByDiaryOrderByTodoOrderAscIdAsc(Diary diary);

    void deleteByDiary(Diary diary);
}
