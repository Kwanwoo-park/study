package spring.study.repository.board;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import spring.study.entity.board.Board;
import spring.study.entity.member.Member;

import java.util.List;


public interface BoardRepository extends JpaRepository<Board, Long> {
    public Page<Board> findByTitle(String title, Pageable pageable);

    public Page<Board> findByMember(Member member, Pageable pageable);

    public Page<Board> findByMemberIn(List<Member> members, Pageable pageable);

    public List<Board> findByMemberIn(List<Member> members);

    @Transactional
    public void deleteByMember(Member member);
}
