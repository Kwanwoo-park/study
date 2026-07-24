package spring.study.diary.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.study.diary.entity.Diary;
import spring.study.diary.entity.DiaryTodo;
import spring.study.diary.repository.DiaryTodoRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DiaryTodoService {
    private final DiaryTodoRepository diaryTodoRepository;

    @Transactional
    public DiaryTodo save(DiaryTodo todo) {
        return diaryTodoRepository.save(todo);
    }

    @Transactional
    public List<DiaryTodo> saveAll(List<DiaryTodo> todos) {
        return diaryTodoRepository.saveAll(todos);
    }

    @Transactional(readOnly = true)
    public List<DiaryTodo> findByDiary(Diary diary) {
        return diaryTodoRepository.findByDiaryOrderByTodoOrderAscIdAsc(diary);
    }

    @Transactional
    public void deleteByDiary(Diary diary) {
        diaryTodoRepository.deleteByDiary(diary);
    }
}
