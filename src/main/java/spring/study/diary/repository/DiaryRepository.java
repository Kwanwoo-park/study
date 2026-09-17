package spring.study.diary.repository;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spring.study.diary.entity.Diary;
import spring.study.member.entity.Member;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiaryRepository extends JpaRepository<Diary, Long> {
    @Transactional(readOnly = true)
    List<Diary> findByMember(Member member, Pageable pageable);

    @Transactional(readOnly = true)
    List<Diary> findByMemberAndTitleContainingIgnoreCase(Member member, String title, Pageable pageable);

    @Transactional(readOnly = true)
    Optional<Diary> findByIdAndMember(Long id, Member member);

    @Transactional(readOnly = true)
    long countByMember(Member member);

    @Transactional(readOnly = true)
    long countByMemberAndTitleContainingIgnoreCase(Member member, String title);

    void deleteByMember(Member member);
}
