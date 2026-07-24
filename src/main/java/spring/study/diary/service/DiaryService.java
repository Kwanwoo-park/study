package spring.study.diary.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.study.diary.dto.DiaryRequestDto;
import spring.study.diary.dto.DiaryResponseDto;
import spring.study.diary.entity.Diary;
import spring.study.diary.repository.DiaryRepository;
import spring.study.member.entity.Member;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DiaryService {
    private final DiaryRepository diaryRepository;

    @Transactional
    public Diary save(Diary diary) {
        return diaryRepository.save(diary);
    }

    @Transactional
    public DiaryResponseDto save(DiaryRequestDto requestDto, Member member) {
        return new DiaryResponseDto(diaryRepository.save(requestDto.toEntity(member)));
    }

    @Transactional(readOnly = true)
    public Diary findById(Long id) {
        return diaryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 일기입니다"));
    }

    @Transactional(readOnly = true)
    public Diary findByIdAndMember(Long id, Member member) {
        return diaryRepository.findByIdAndMember(id, member)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 일기입니다"));
    }

    @Transactional(readOnly = true)
    public List<Diary> findByMember(Member member, int page, int size) {
        if (page < 0 || size <= 0) {
            throw new IllegalArgumentException("페이지와 조회 개수를 확인해 주세요");
        }

        Sort sort = Sort.by(Sort.Order.desc("registerTime"), Sort.Order.desc("id"));
        return diaryRepository.findByMember(member, PageRequest.of(page, size, sort));
    }

    @Transactional(readOnly = true)
    public List<DiaryResponseDto> findResponseByMember(Member member, int page, int size) {
        return findByMember(member, page, size).stream()
                .map(DiaryResponseDto::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countByMember(Member member) {
        return diaryRepository.countByMember(member);
    }

    @Transactional
    public void delete(Diary diary) {
        diaryRepository.delete(diary);
    }

    @Transactional
    public void deleteByMember(Member member) {
        diaryRepository.deleteByMember(member);
    }
}
