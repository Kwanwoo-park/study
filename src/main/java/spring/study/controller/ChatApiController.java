package spring.study.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.dto.chat.ChatRoomRequestDto;
import spring.study.entity.ChatRoom;
import spring.study.entity.ChatRoomMember;
import spring.study.entity.Member;
import spring.study.service.ChatRoomMemberService;
import spring.study.service.ChatRoomService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
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

        System.out.println(name);

        ChatRoom room = roomService.createRoom(name);

        roomMemberService.save(member, room);

        return ResponseEntity.ok(room);
    }
}
