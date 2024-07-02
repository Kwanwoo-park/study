package spring.study.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import spring.study.dto.board.BoardRequestDto;
import spring.study.dto.board.BoardResponseDto;
import spring.study.entity.Board;
import spring.study.entity.Member;
import spring.study.repository.BoardRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class BoardService {
    private final BoardRepository boardRepository;

    @Transactional
    public Long save(BoardRequestDto boardSaveDto) {
        return boardRepository.save(boardSaveDto.toEntity()).getId();
    }

    @Transactional
    public Board save(Board board) {
        return boardRepository.save(board);
    };

    public HashMap<String, Object> findAll(Integer page, Integer size) {
        HashMap<String, Object> resultMap = new HashMap<>();

        Page<Board> list = boardRepository.findAll(PageRequest.of(page, size, Sort.by("id").descending()));

        resultMap.put("list", list.stream().map(BoardResponseDto::new).collect(Collectors.toList()));
        resultMap.put("paging", list.getPageable());
        resultMap.put("totalCnt", list.getTotalElements());
        resultMap.put("totalPage", list.getTotalPages());

        return resultMap;
    }

    public Board findById(Long id) {
        return boardRepository.findById(id).orElseThrow();
    }

    public Board findBoard(Long id) {
        return boardRepository.findById(id).orElseThrow();
    }

    @Transactional
    public int updateBoard(Long id, String title, String content) {
        Board board = boardRepository.findById(id).orElseThrow(() -> new RuntimeException(
                "존재하지 않는 게시글입니다."
        ));

        board.changeContent(content);
        board.changeTitle(title);
        board.changeUpdateTime(LocalDateTime.now());

        return board.getId().intValue();
    }

    @Transactional
    public void updateBoardReadCntInc(Long id) {
        Board board = boardRepository.findById(id).orElseThrow(() -> new RuntimeException(
                "존재하지 않는 게시글입니다."
        ));

        board.changeReadCnt();
    }

    public void deleteById(Long id) {
        boardRepository.deleteById(id);
    }

    public void deleteAll(Long[] deleteId) {
        boardRepository.deleteBoard(deleteId);
    }
}
