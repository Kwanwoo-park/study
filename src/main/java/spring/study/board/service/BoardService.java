package spring.study.board.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import spring.study.board.dto.BoardRequestDto;
import spring.study.board.dto.BoardResponseDto;
import spring.study.board.entity.Board;
import spring.study.follow.entity.Follow;
import spring.study.member.entity.Member;
import spring.study.board.repository.BoardRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class BoardService {
    @Qualifier("boardRedisTemplate")
    private final RedisTemplate<String, BoardResponseDto> redisTemplate;
    private static final String FEED_CACHE_KEY = "feed:recent";

    private final BoardRepository boardRepository;

    @Transactional
    public Long save(BoardRequestDto boardSaveDto) {
        return boardRepository.save(boardSaveDto.toEntity()).getId();
    }

    @Transactional
    public Board save(Board board) {
        return boardRepository.save(board);
    }

    public List<BoardResponseDto> getBoard(LocalDateTime cursor, int limit) {
        List<BoardResponseDto> cached = getFromCache(limit);

        if (!cached.isEmpty() && (cursor == null || cursor.isAfter(cached.get(cached.size()-1).getRegisterTime())))
            return cached;

        List<BoardResponseDto> dbPosts = boardRepository.findNextBoard(cursor, PageRequest.of(0, limit));

        if (cursor == null && !dbPosts.isEmpty())
            saveToCache(dbPosts);

        return dbPosts;
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

    public HashMap<String, Object> findByMembers(Member member, Integer page, Integer size) {
        HashMap<String, Object> resultMap = new HashMap<>();

        List<Member> memberList = new ArrayList<>();

        memberList.add(member);

        for (Follow follow : member.getFollower()) {
            memberList.add(follow.getFollowing());
        }

        Page<Board> list = boardRepository.findByMemberIn(memberList, PageRequest.of(page, size, Sort.by("id").descending()));

        resultMap.put("list", list.stream().map(BoardResponseDto::new).collect(Collectors.toList()));
        resultMap.put("paging", list.getPageable());
        resultMap.put("totalCnt", list.getTotalElements());
        resultMap.put("totalPage", list.getTotalPages());

        return resultMap;
    }

    public List<Board> findByMembers(Member member) {
        List<Member> memberList = new ArrayList<>();

        memberList.add(member);

        for (Follow follow : member.getFollower()) {
            memberList.add(follow.getFollowing());
        }

        return boardRepository.findByMemberIn(memberList, Sort.by("id").descending());
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

    private void saveToCache(List<BoardResponseDto> boards) {
        redisTemplate.delete(FEED_CACHE_KEY);
        redisTemplate.opsForList().rightPushAll(FEED_CACHE_KEY, boards);
        redisTemplate.expire(FEED_CACHE_KEY, Duration.ofMinutes(5));
    }

    private List<BoardResponseDto> getFromCache(int limit) {
        List<BoardResponseDto> range = redisTemplate.opsForList().range(FEED_CACHE_KEY,0, limit-1);
        if (range == null || range.isEmpty()) return Collections.emptyList();
        else return range;
    }
}
