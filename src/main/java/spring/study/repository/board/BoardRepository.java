package spring.study.repository.board;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import spring.study.entity.board.Board;
import spring.study.entity.member.Member;


public interface BoardRepository extends JpaRepository<Board, Long> {
    public Page<Board> findByTitle(String title, Pageable pageable);

    public Page<Board> findByMember(Member member, Pageable pageable);

    @Transactional
    public void deleteByMember(Member member);
}
