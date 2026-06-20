package spring.study.admin.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import spring.study.board.entity.Board;
import spring.study.board.service.BoardService;
import spring.study.chat.entity.ChatMessage;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.service.ChatMessageService;
import spring.study.member.entity.Member;
import spring.study.member.service.MemberService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminFacade {
    private final MemberService memberService;
    private final BoardService boardService;
    private final ChatMessageService chatMessageService;
    private final RedisTemplate<String, String> redisTemplate;

    public ResponseEntity<?> memberOnline() {
        Set<String> onlineUserKeys = redisTemplate.keys("online:user:*");

        List<Long> userIds = (onlineUserKeys == null ? Set.<String>of() : onlineUserKeys)
                .stream()
                .map(k -> k.substring(k.lastIndexOf(":") + 1))
                .map(Long::parseLong)
                .toList();
        long count = userIds.size();

        if (count == 0L) {
            redisTemplate.delete("online:total");
        } else {
            redisTemplate.opsForValue().set("online:total", Long.toString(count));
        }

        List<Member> list = memberService.findMember(userIds);

        return ResponseEntity.ok(Map.of(
                "result", 1L,
                "count", count,
                "list", list
        ));
    }

    public ResponseEntity<?> newMember() {
        LocalDateTime end = LocalDateTime.now(), start = end.minusDays(1);

        List<Member> list = memberService.findNewUser(start, end);
        int count = list.size();

        return ResponseEntity.ok(Map.of(
                "result", 1L,
                "count", count,
                "list", list
        ));
    }

    public ResponseEntity<?> newBoard() {
        LocalDateTime end = LocalDateTime.now(), start = end.minusDays(7);

        List<Board> list = boardService.findNewBoard(start, end);
        int count = list.size();

        return ResponseEntity.ok(Map.of(
                "result", 1L,
                "count", count,
                "list", list
        ));
    }

    public ResponseEntity<?> chattingActive() {
        LocalDateTime end = LocalDateTime.now(), start = end.minusHours(1);

        List<ChatRoom> list = chatMessageService.findActiveChatting(start, end).stream()
                .map(ChatMessage::getRoom)
                .distinct()
                .toList();
        int count = list.size();

        return ResponseEntity.ok(Map.of(
                "result", 1L,
                "count", count,
                "list", list
        ));
    }
}
