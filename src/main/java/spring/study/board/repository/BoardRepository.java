package spring.study.board.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import spring.study.board.entity.Board;
import spring.study.common.entity.CommonVisibility;
import spring.study.member.entity.Member;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {
    @Transactional(readOnly = true)
    List<Board> findByMemberIn(List<Member> list, Pageable pageable);

    @Transactional(readOnly = true)
    List<Board> findByMember(Member member, Pageable pageable);

    @Transactional(readOnly = true)
    List<Board> findByMemberAndVisibility(Member member, CommonVisibility visibility, Pageable pageable);

    @Transactional(readOnly = true)
    List<Board> findByMember(Member member, Sort sort);

    @Transactional(readOnly = true)
    List<Board> findByMember(Member members);

    @Transactional(readOnly = true)
    List<Board> findByRegisterTimeBetween(LocalDateTime start, LocalDateTime end);

    @Transactional(readOnly = true)
    long countByMember(Member member);

    @Transactional(readOnly = true)
    long countByMemberAndVisibility(Member member, CommonVisibility visibility);
    @Transactional(readOnly = true)
    long countByMemberIn(List<Member> members);

    @Transactional
    void deleteByMember(Member member);

    @Transactional(readOnly = true)
    Optional<Board> findFirstByMemberAndIdGreaterThanOrderByIdAsc(Member member, Long id);
    @Transactional(readOnly = true)
    Optional<Board> findFirstByMemberAndIdLessThanOrderByIdDesc(Member member, Long id);
    @Transactional(readOnly = true)
    Optional<Board> findFirstByMemberAndVisibilityAndIdGreaterThanOrderByIdAsc(Member member, CommonVisibility visibility, Long id);
    @Transactional(readOnly = true)
    Optional<Board> findFirstByMemberAndVisibilityAndIdLessThanOrderByIdDesc(Member member, CommonVisibility visibility, Long id);
}
