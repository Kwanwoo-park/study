package spring.study.board.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.study.board.dto.BoardRequestDto;
import spring.study.board.dto.BoardResponseDto;
import spring.study.board.entity.Board;
import spring.study.common.entity.CommonVisibility;
import spring.study.common.exception.ResourceNotFoundException;
import spring.study.member.entity.Member;
import spring.study.board.repository.BoardRepository;

import java.time.LocalDateTime;
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

    @Transactional
    public Board save(Board board) {
        return boardRepository.save(board);
    }

    public List<Board> findNewBoard(LocalDateTime start, LocalDateTime end) {
        return boardRepository.findByRegisterTimeBetween(start, end);
    }

    public List<Board> getBoard(int cursor, int limit, List<Member> list) {
        return boardRepository.findByMemberIn(list, PageRequest.of(cursor, limit, Sort.by("registerTime").descending()));
    }

    public List<Board> getBoardByMember(int cursor, int limit, Member member) {
        return getBoardByMember(cursor, limit, member, true);
    }

    public List<Board> getBoardByMember(int cursor, int limit, Member member, boolean includePrivate) {
        PageRequest pageable = PageRequest.of(cursor, limit, Sort.by("registerTime").descending());
        if (includePrivate) {
            return boardRepository.findByMember(member, pageable);
        }
        return boardRepository.findByMemberAndVisibility(member, CommonVisibility.PUBLIC, pageable);
    }

    public HashMap<String, Object> findAll(Integer page, Integer size) {
        HashMap<String, Object> resultMap = new HashMap<>();

        Page<Board> list = boardRepository.findAll(PageRequest.of(page, size, Sort.by("id").descending()));

        resultMap.put("list", list.stream().map(BoardResponseDto::new).collect(Collectors.toList()));
        resultMap.put("paging", list.getPageable());
        resultMap.put("totalCnt", list.getTotalElements());
        resultMap.put("totalPage", list.getTotalPages());

        return resultMap;
    }

    public List<Board> findByMember(Member member) {
        return boardRepository.findByMember(member, Sort.by("id").descending());
    }

    public List<Board> findAll() {
        return boardRepository.findAll();
    }

    public Board findById(Long id) {
        return boardRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 게시글입니다"));
    }

    public Boolean existBoard(Long id) {
        return boardRepository.existsById(id);
    }

    public Long countByMember(Member member) {
        return boardRepository.countByMember(member);
    }

    public long countByMember(Member member, boolean includePrivate) {
        if (includePrivate) {
            return boardRepository.countByMember(member);
        }
        return boardRepository.countByMemberAndVisibility(member, CommonVisibility.PUBLIC);
    }

    public long countByMembers(List<Member> members) {
        return members.isEmpty() ? 0L : boardRepository.countByMemberIn(members);
    }

    public long[] getBoardIdList(Long id, Member member) {
        return getBoardIdList(id, member, true);
    }

    public long[] getBoardIdList(Long id, Member member, boolean includePrivate) {
        Board previous = includePrivate
                ? boardRepository.findFirstByMemberAndIdGreaterThanOrderByIdAsc(member, id).orElse(null)
                : boardRepository.findFirstByMemberAndVisibilityAndIdGreaterThanOrderByIdAsc(
                        member, CommonVisibility.PUBLIC, id).orElse(null);
        Board next = includePrivate
                ? boardRepository.findFirstByMemberAndIdLessThanOrderByIdDesc(member, id).orElse(null)
                : boardRepository.findFirstByMemberAndVisibilityAndIdLessThanOrderByIdDesc(
                        member, CommonVisibility.PUBLIC, id).orElse(null);
        return new long[]{previous == null ? 0L : previous.getId(), next == null ? 0L : next.getId()};
    }

    @Transactional
    public long updateBoard(Long id, String content, CommonVisibility visibility) {
        Board board = boardRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(
                "존재하지 않는 게시글입니다."
        ));

        board.changeContent(content);
        board.changeVisibility(visibility);
        board.changeUpdateTime(LocalDateTime.now());

        return board.getId();
    }

    @Transactional
    public long updateBoard(Long id, String content) {
        return updateBoard(id, content, null);
    }

    @Transactional
    public void deleteById(Long id) {
        boardRepository.deleteById(id);
    }

    public void deleteByMember(Member member) {
        boardRepository.deleteByMember(member);
    }
}
