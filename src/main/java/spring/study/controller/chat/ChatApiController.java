package spring.study.controller.chat;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import spring.study.dto.member.MemberRequestDto;
import spring.study.entity.chat.ChatRoom;
import spring.study.entity.chat.ChatRoomMember;
import spring.study.entity.member.Member;
import spring.study.service.chat.ChatMessageService;
import spring.study.service.chat.ChatRoomMemberService;
import spring.study.service.chat.ChatRoomService;
import spring.study.service.member.MemberService;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatApiController {
    private final ChatRoomService roomService;
    private final ChatRoomMemberService roomMemberService;
    private final MemberService memberService;
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

        ChatRoom room = roomService.createRoom(name, 1L);

        roomMemberService.save(member, room);

        return ResponseEntity.ok(room);
    }

    @PostMapping("/create")
    public ResponseEntity<ChatRoom> createRoomByOnetoOne(@RequestBody MemberRequestDto memberRequestDto, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return ResponseEntity.status(501).body(null);

        member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return ResponseEntity.status(501).body(null);
        }

        Member search_Member = memberService.findMember(memberRequestDto.getEmail());

        Long id = member.getId() + search_Member.getId();
        ChatRoom search = roomService.findByName(String.valueOf(id));

        if (search != null)
            return ResponseEntity.ok(search);

        ChatRoom room = roomService.createRoom(String.valueOf(id), 2L);

        roomMemberService.save(member, room);
        roomMemberService.save(search_Member, room);

        return ResponseEntity.ok(room);
    }

    @PostMapping("/sendImage")
    public ResponseEntity<HashMap<String, String>> sendImage(@RequestPart MultipartFile file, HttpServletRequest request) throws IOException {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return ResponseEntity.status(501).body(null);

        member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return ResponseEntity.status(501).body(null);
        }

        String fileDir = "/home/ec2-user/app/step/study/src/main/resources/static/img/";
        //String fileDir = "/Users/lg/Desktop/study/study/src/main/resources/static/img/";

        File f = new File(fileDir + file.getOriginalFilename());

        if (!f.exists()) {
            file.transferTo(f);
        }

        HashMap<String, String> map = new HashMap<>();
        map.put("name", file.getOriginalFilename());

        return ResponseEntity.ok(map);
    }
}
