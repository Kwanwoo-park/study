package spring.study.entity.board;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import spring.study.dto.board.BoardRequestDto;

public interface BoardRepository extends JpaRepository<Board, Long> {
    static final String update_board = "update board " +
            "set title = :#{#boardRequestDto.title}, " +
            "content = :#{#boardRequestDto.content}, " +
            "update_time = NOW() " +
            "where id = :#{#boardRequestDto.id}";

    static final String update_board_read_cnt_inc = "update board "
            + "set read_cnt = read_cnt + 1 "
            + "where id = :id";

    static final String delete_board = "delete from board "
            + "where id in (:deleteList)";

    @Transactional
    @Modifying
    @Query(value = update_board, nativeQuery = true)
    public int updateBoard(@Param("boardRequestDto")BoardRequestDto boardRequestDto);

    @Transactional
    @Modifying
    @Query(value = update_board_read_cnt_inc, nativeQuery = true)
    public int updateBoardReadCntInc(@Param("id") Long id);

    @Transactional
    @Modifying
    @Query(value = delete_board, nativeQuery = true)
    public int deleteBoard(@Param("deleteList") Long[] deleteList);
}
