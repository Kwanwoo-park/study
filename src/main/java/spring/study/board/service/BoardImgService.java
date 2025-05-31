package spring.study.board.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import spring.study.board.entity.Board;
import spring.study.board.entity.BoardImg;
import spring.study.board.repository.BoardImgRepository;

import java.util.List;

@RequiredArgsConstructor
@Service
public class BoardImgService {
    private final BoardImgRepository boardImgRepository;

    @Transactional
    public BoardImg save(BoardImg boardImg) {
        return boardImgRepository.save(boardImg);
    }

    public List<BoardImg> findBoard(Board board) {
        return boardImgRepository.findByBoard(board);
    }

    public void deleteBoard(Board board) {
        boardImgRepository.deleteByBoard(board);
    }
}
