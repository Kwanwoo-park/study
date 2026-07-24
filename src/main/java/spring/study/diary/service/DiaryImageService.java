package spring.study.diary.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.study.diary.entity.Diary;
import spring.study.diary.entity.DiaryImage;
import spring.study.diary.repository.DiaryImageRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DiaryImageService {
    private final DiaryImageRepository diaryImageRepository;

    @Transactional
    public DiaryImage save(DiaryImage image) {
        return diaryImageRepository.save(image);
    }

    @Transactional
    public List<DiaryImage> saveAll(List<DiaryImage> images) {
        return diaryImageRepository.saveAll(images);
    }

    @Transactional(readOnly = true)
    public List<DiaryImage> findByDiary(Diary diary) {
        return diaryImageRepository.findByDiaryOrderByIdAsc(diary);
    }

    @Transactional
    public void deleteByDiary(Diary diary) {
        diaryImageRepository.deleteByDiary(diary);
    }
}
