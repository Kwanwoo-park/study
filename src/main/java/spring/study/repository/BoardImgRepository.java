package spring.study.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import spring.study.entity.Board;
import spring.study.entity.BoardImg;

import java.util.List;

public interface BoardImgRepository extends JpaRepository<BoardImg, Long> {
    public List<BoardImg> findByBoard(Board board);

    @Transactional
    public void deleteByBoard(Board board);
}