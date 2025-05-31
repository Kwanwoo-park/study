package spring.study.board.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import spring.study.board.entity.Board;
import spring.study.member.entity.Member;

import java.util.List;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {
    List<Board> findByMember(Member member, Sort sort);

    Page<Board> findByMemberIn(List<Member> members, Pageable pageable);

    List<Board> findByMemberIn(List<Member> members, Sort sort);

    List<Board> findByMemberIn(List<Member> members);

    @Transactional
    void deleteByMember(Member member);
}
