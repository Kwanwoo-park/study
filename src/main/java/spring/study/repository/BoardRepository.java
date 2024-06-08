package spring.study.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import spring.study.dto.board.BoardRequestDto;
import spring.study.entity.Board;

public interface BoardRepository extends JpaRepository<Board, Long> {

    static final String delete_board = "delete from board "
            + "where id in (:deleteList)";

    @Transactional
    @Modifying
    @Query(value = delete_board, nativeQuery = true)
    public int deleteBoard(@Param("deleteList") Long[] deleteList);

    public Board findByTitle(String title);
}
