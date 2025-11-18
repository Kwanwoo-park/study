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
import spring.study.follow.entity.Follow;
import spring.study.member.entity.Member;
import spring.study.board.repository.BoardRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

    public List<BoardResponseDto> getBoard(int cursor, int limit, Member member) {
        List<Member> list = getMemberList(member);

        return boardRepository.findByMemberIn(list, PageRequest.of(cursor, limit, Sort.by("registerTime").descending())).stream().map(BoardResponseDto::new).toList();
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

    private List<Member> getMemberList(Member member) {
        List<Member> memberList = new ArrayList<>();

        memberList.add(member);

        for (Follow follow : member.getFollower()) {
            memberList.add(follow.getFollowing());
        }

        return memberList;
    }

    public List<Board> findAll() {
        return boardRepository.findAll();
    }

    public Board findById(Long id) {
        return boardRepository.findById(id).orElseThrow();
    }

    public Boolean existBoard(Long id) {
        return boardRepository.existsById(id);
    }

    public long[] getBoardIdList(Long id, Member member) {
        List<Long> id_list = member.getBoard().stream().map(Board::getId).toList();

        int size = id_list.size();
        long[] ids = new long[2];

        if (size > 1) {
            int idx = id_list.indexOf(id);

            if (idx == 0) ids[0] = id_list.get(idx+1);
            else if (idx == size-1) ids[1] = id_list.get(idx-1);
            else {
                ids[0] = id_list.get(idx+1);
                ids[1] = id_list.get(idx-1);
            }
        }

        return ids;
    }

    @Transactional
    public int updateBoard(Long id, String content) {
        Board board = boardRepository.findById(id).orElseThrow(() -> new RuntimeException(
                "존재하지 않는 게시글입니다."
        ));

        board.changeContent(content);
        board.changeUpdateTime(LocalDateTime.now());

        return board.getId().intValue();
    }

    public void deleteById(Long id) {
        boardRepository.deleteById(id);
    }

    public void deleteByMember(Member member) {
        boardRepository.deleteByMember(member);
    }
}
