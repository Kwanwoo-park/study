package spring.study.controller.chat;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.entity.chat.ChatRoom;
import spring.study.entity.member.Member;
import spring.study.service.chat.ChatRoomMemberService;
import spring.study.service.chat.ChatRoomService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatApiController {
    private final ChatRoomService roomService;
    private final ChatRoomMemberService roomMemberService;
    private Member member;

    @PostMapping("/createRoom")
    public ResponseEntity<ChatRoom> createRoom(@RequestParam String name, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return ResponseEntity.status(501).body(null);

        member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return ResponseEntity.status(501).body(null);
        }

        ChatRoom room = roomService.createRoom(name);

        roomMemberService.save(member, room);

        return ResponseEntity.ok(room);
    }
}
