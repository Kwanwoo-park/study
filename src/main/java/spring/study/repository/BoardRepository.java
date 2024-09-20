package spring.study.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import spring.study.entity.Board;
import spring.study.entity.Member;

import java.util.List;


public interface BoardRepository extends JpaRepository<Board, Long> {
    public List<Board> findByTitle(String title);

    @Transactional
    public void deleteByMember(Member member);
}
