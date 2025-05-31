package spring.study.service.board;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import spring.study.entity.board.Board;
import spring.study.entity.board.BoardImg;
import spring.study.repository.board.BoardImgRepository;

import java.util.Arrays;
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
