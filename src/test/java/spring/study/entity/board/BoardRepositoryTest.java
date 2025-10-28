package spring.study.entity.board;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import spring.study.StudyApplication;
import spring.study.board.dto.BoardResponseDto;
import spring.study.board.entity.Board;
import spring.study.board.repository.BoardRepository;
import spring.study.follow.entity.Follow;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.repository.MemberRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(classes = {StudyApplication.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE,
        connection = EmbeddedDatabaseConnection.H2)
public class BoardRepositoryTest {
    @Autowired
    BoardRepository boardRepository;
    @Autowired
    MemberRepository memberRepository;
    @Qualifier("boardRedisTemplate")
    @Autowired
    RedisTemplate<String, BoardResponseDto> redisTemplate;

    @Test
    void save() {
        // given
        Member member = Member.builder()
                .email("test2@test.com")
                .pwd("test")
                .name("test")
                .phone("010-1234-1234")
                .birth("1900-01-01")
                .role(Role.USER)
                .profile("1.jpg")
                .build();

        Member save = memberRepository.save(member);

        Board board = Board.builder()
                .title("test")
                .content("test")
                .build();

        save.addBoard(board);

        // when
        Board saveBoard = boardRepository.save(board);

        // then
        assertThat(saveBoard.getContent()).isEqualTo(board.getContent());
        assertThat(saveBoard.getMember()).isEqualTo(save);
        assertThat(save.getBoard().get(0)).isEqualTo(saveBoard);
    }

    @Test
    void findNextBoard() {
        // given
        LocalDateTime cursor = LocalDateTime.now();

        // when
        List<BoardResponseDto> list = boardRepository.findNextBoard(cursor, PageRequest.of(0, 10));

        // then
        if (!list.isEmpty()) {
            for (BoardResponseDto b : list)
                System.out.println(b.getId() + " " + b.getContent() + " " + b.getMember().getEmail());
        } else {
            System.out.println("List is empty");
        }

        saveToCache(list);
        System.out.println(getFromCache(10).get(0).getFavorites());
    }

    private void saveToCache(List<BoardResponseDto> boards) {
        redisTemplate.delete("feed:recent");
        redisTemplate.opsForList().rightPushAll("feed:recent", boards);
        redisTemplate.expire("feed:recent", Duration.ofMinutes(5));
    }

    private List<BoardResponseDto> getFromCache(int limit) {
        List<BoardResponseDto> range = redisTemplate.opsForList().range("feed:recent", 0, limit-1);
        if (range == null || range.isEmpty()) return Collections.emptyList();
        else return range;
    }

    @Test
    void findAll() {
        // given
        Member member = memberRepository.findByEmail("test2@test.com").orElseThrow();

        Board board1 = Board.builder()
                .content("test")
                .build();

        Board board2 = Board.builder()
                .content("test")
                .build();

        member.addBoard(board1);
        member.addBoard(board2);

        Board saveBoard1 = boardRepository.save(board1);
        Board saveBoard2 = boardRepository.save(board2);

        // when
        List<Board> result = boardRepository.findAll(Sort.by("id").ascending());

        // then
        for (Board b : result)
            System.out.println(b.getId() + " " + b.getContent());
    }

    @Test
    void findMemberList() {
        //given
        List<Follow> following =memberRepository.findByEmail("test@test.com").orElseThrow().getFollower();

        List<Member> memberList = new ArrayList<>();

        for (Follow follow : following) {
            memberList.add(follow.getFollowing());
        }

        // when
        Page<Board> boards = boardRepository.findByMemberIn(memberList, PageRequest.of(0, 10, Sort.by("id").descending()));

        // then
        for (Board b : boards) {
            System.out.println(b.getId() + " " + b.getMember().getEmail());
        }
    }

    @Test
    void deleteByMember() {
        // given
        Member member = memberRepository.findByEmail("test@test.com").orElseThrow();
        System.out.println(boardRepository.findAll().size());

        // when

        boardRepository.deleteByMember(member);

        // then
        System.out.println(boardRepository.findAll().size());
    }
}
