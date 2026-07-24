package spring.study.diary.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spring.study.diary.entity.Diary;
import spring.study.member.entity.Member;

import java.util.List;
import java.util.Optional;

@Repository
public interface DiaryRepository extends JpaRepository<Diary, Long> {
    List<Diary> findByMember(Member member, Pageable pageable);

    Optional<Diary> findByIdAndMember(Long id, Member member);

    long countByMember(Member member);

    void deleteByMember(Member member);
}
