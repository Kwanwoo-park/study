package spring.study.board.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spring.study.board.entity.Board;
import spring.study.board.entity.BoardImg;

import java.util.List;

@Repository
public interface BoardImgRepository extends JpaRepository<BoardImg, Long> {
    List<BoardImg> findByBoard(Board board);

    @Transactional
    void deleteByBoard(Board board);
}
