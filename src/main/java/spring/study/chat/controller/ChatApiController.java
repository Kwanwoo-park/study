package spring.study.chat.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import spring.study.member.dto.MemberRequestDto;
import spring.study.chat.entity.ChatRoom;
import spring.study.member.entity.Member;
import spring.study.aws.service.ImageS3Service;
import spring.study.chat.service.ChatRoomMemberService;
import spring.study.chat.service.ChatRoomService;
import spring.study.member.service.MemberService;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatApiController {
    private final ChatRoomService roomService;
    private final ChatRoomMemberService roomMemberService;
    private final MemberService memberService;
    private final ImageS3Service imageS3Service;

    @PostMapping("/createRoom")
    public ResponseEntity<ChatRoom> createRoom(@RequestParam String name, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return ResponseEntity.status(501).body(null);

        Member member = (Member) session.getAttribute("member");

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
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);
        }

        Member search_Member = memberService.findMember(memberRequestDto.getEmail());

        ChatRoom search = roomService.findByName(member, search_Member);

        if (search != null)
            return ResponseEntity.ok(search);

        String name1 = member.getEmail() + " " + search_Member.getEmail();

        ChatRoom room = roomService.createRoom(name1, 2L);

        roomMemberService.save(member, room);
        roomMemberService.save(search_Member, room);

        return ResponseEntity.ok(room);
    }

    @PostMapping("/sendImage")
    public ResponseEntity<HashMap<String, String>> sendImage(@RequestPart MultipartFile file, HttpServletRequest request) throws IOException, FileNotFoundException {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);
        }

        if (imageS3Service.fileFormatCheck(file))
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);

        HashMap<String, String> map = new HashMap<>();

        String imageUrl = imageS3Service.uploadImageToS3(file);

        map.put("name", imageUrl);

        return ResponseEntity.ok(map);
    }
}
