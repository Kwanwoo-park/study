package spring.study.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.study.dto.board.BoardRequestDto;
import spring.study.dto.board.BoardResponseDto;
import spring.study.entity.Board;
import spring.study.repository.BoardRepository;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class BoardService {
    private final BoardRepository boardRepository;

    @Transactional
    public Long save(BoardRequestDto boardSaveDto) {
        return boardRepository.save(boardSaveDto.toEntity()).getId();
    }

    @Transactional(readOnly = true)
    public HashMap<String, Object> findAll(Integer page, Integer size) {
        HashMap<String, Object> resultMap = new HashMap<>();

        Page<Board> list = boardRepository.findAll(PageRequest.of(page, size, Sort.by("id").descending()));

        resultMap.put("list", list.stream().map(BoardResponseDto::new).collect(Collectors.toList()));
        resultMap.put("paging", list.getPageable());
        resultMap.put("totalCnt", list.getTotalElements());
        resultMap.put("totalPage", list.getTotalPages());

        return resultMap;
    }

    public BoardResponseDto findById(Long id) {
        return new BoardResponseDto(boardRepository.findById(id).get());
    }

    public List<Board> findName(String name) { return boardRepository.findByRegisterName(name); }

    public List<Board> findEmail(String email) { return boardRepository.findByRegisterEmail(email); }

    public int updateBoard(BoardRequestDto boardRequestDto) {
        return boardRepository.updateBoard(boardRequestDto);
    }

    public int updateBoardReadCntInc(Long id) {
        return boardRepository.updateBoardReadCntInc(id);
    }

    public void deleteById(Long id) {
        boardRepository.deleteById(id);
    }

    public void deleteAll(Long[] deleteId) {
        boardRepository.deleteBoard(deleteId);
    }
}
