package spring.study.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import spring.study.entity.Board;
import spring.study.entity.Member;


public interface BoardRepository extends JpaRepository<Board, Long> {
    public Board findByTitle(String title);

    @Transactional
    public void deleteByMember(Member member);
}
