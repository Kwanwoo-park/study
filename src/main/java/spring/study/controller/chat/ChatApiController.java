package spring.study.controller.chat;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import spring.study.dto.member.MemberRequestDto;
import spring.study.entity.chat.ChatRoom;
import spring.study.entity.member.Member;
import spring.study.service.chat.ChatRoomMemberService;
import spring.study.service.chat.ChatRoomService;
import spring.study.service.member.MemberService;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatApiController {
    private final ChatRoomService roomService;
    private final ChatRoomMemberService roomMemberService;
    private final MemberService memberService;
    private Member member;

    @Value("${img.path}")
    String fileDir;

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

        String name1 = member.getEmail() + " " + search_Member.getEmail();
        String name2 = search_Member.getEmail() + " " + member.getEmail();

        ChatRoom search1 = roomService.findByName(name1);
        ChatRoom search2 = roomService.findByName(name2);

        if (search1 != null)
            return ResponseEntity.ok(search1);

        if (search2 != null)
            return ResponseEntity.ok(search2);

        ChatRoom room = roomService.createRoom(name1, 2L);

        roomMemberService.save(member, room);
        roomMemberService.save(search_Member, room);

        return ResponseEntity.ok(room);
    }

    @PostMapping("/sendImage")
    public ResponseEntity<HashMap<String, String>> sendImage(@RequestPart MultipartFile file, HttpServletRequest request) throws IOException, FileNotFoundException {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return ResponseEntity.status(501).body(null);

        member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return ResponseEntity.status(501).body(null);
        }

        String format = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String[] formatArr = {"jpg", "jpeg", "png", "gif", "tif", "tiff"};

        if (!Arrays.stream(formatArr).toList().contains(format))
            return ResponseEntity.status(500).body(null);

        HashMap<String, String> map = new HashMap<>();

        File f = new File(fileDir + file.getOriginalFilename());
        try {
            if (!f.exists()) {
                file.transferTo(f);

                if (!f.exists())
                    return ResponseEntity.status(500).body(null);
            }

            map.put("name", file.getOriginalFilename());
        }
        catch (FileNotFoundException e) {
            log.debug(e.getMessage());
            return ResponseEntity.status(500).body(null);
        }

        return ResponseEntity.ok(map);
    }
}
