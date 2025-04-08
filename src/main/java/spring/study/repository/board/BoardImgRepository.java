package spring.study.repository.board;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import spring.study.entity.board.Board;
import spring.study.entity.board.BoardImg;

import java.util.List;

public interface BoardImgRepository extends JpaRepository<BoardImg, Long> {
    List<BoardImg> findByBoard(Board board);

    @Transactional
    void deleteByBoard(Board board);
}
