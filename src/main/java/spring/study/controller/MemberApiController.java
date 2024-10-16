package spring.study.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import spring.study.dto.member.MemberRequestDto;
import spring.study.dto.member.MemberResponseDto;
import spring.study.entity.*;
import spring.study.service.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/member")
@Slf4j
public class MemberApiController {
    private final MemberService memberService;
    private final BoardService boardService;
    private final CommentService commentService;
    private final FollowService followService;
    private final ChatRoomMemberService roomMemberService;
    private final ChatMessageService messageService;
    private final UserService userService;
    private Member member;

    @PostMapping("/login/action")
    public ResponseEntity<Member> loginAction(@RequestBody MemberRequestDto dto, HttpServletRequest request) {
        member = (Member) memberService.loadUserByUsername(dto.getEmail());

        if (member == null)
            return ResponseEntity.status(501).body(null);

        if (new BCryptPasswordEncoder().matches(dto.getPassword(), member.getPassword())) {
            if (member.getRole() != Role.DENIED) {
                memberService.updateLastLoginTime(member.getId());
                request.getSession().setAttribute("member", member);

                return ResponseEntity.status(HttpStatus.OK).body(member);
            }
        }

        return ResponseEntity.status(501).body(null);
    }

    @PostMapping("/register/action")
    public ResponseEntity<MemberResponseDto> registerAction(@RequestBody MemberRequestDto memberRequestDto) throws Exception {
        return ResponseEntity.ok(userService.createUser(memberRequestDto));
    }

    @PatchMapping("/detail/action")
    public ResponseEntity<Member> detailAction(@RequestParam MultipartFile file, HttpServletRequest request) throws IOException {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return ResponseEntity.status(501).body(null);

        member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return ResponseEntity.status(501).body(null);
        }

        String fileDir = "/Users/lg/Desktop/study/study/src/main/resources/static/img/";

        File f = new File(fileDir + file.getOriginalFilename());

        if (!f.exists()) {
            file.transferTo(new File(fileDir + file.getOriginalFilename()));
        }

        member.setProfile(file.getOriginalFilename());
        memberService.updateProfile(member.getId(), file.getOriginalFilename());

        session.setAttribute("member", member);

        return ResponseEntity.ok(member);
    }

    @GetMapping("/find")
    public ResponseEntity<Member> findAction(@RequestParam() String email) {
        member = memberService.findMember(email);

        return ResponseEntity.ok(member);
    }

    @GetMapping("/find/info/{phone}&{birth}/action")
    public ResponseEntity<Member> findAction(@PathVariable String phone, @PathVariable String birth) {
        String regEx = "(\\d{3})(\\d{3,4})(\\d{4})";
        phone = phone.replaceAll(regEx, "$1-$2-$3");
        member = memberService.findMember(phone, birth);

        return ResponseEntity.ok(member);
    }

    @PatchMapping("/updatePassword/action")
    public ResponseEntity<Integer> updatePasswordAction(@RequestBody MemberRequestDto memberUpdateDto, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid()) {
            member = memberService.findMember(memberUpdateDto.getEmail());
        }
        else {
            member = (Member) session.getAttribute("member");
            session.invalidate();

            if (member == null)
                return ResponseEntity.status(501).body(null);
        }

        int result = userService.updatePwd(member.getId(), memberUpdateDto.getPassword());

        member = null;

        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/withdrawal/action")
    public ResponseEntity<Member> withdrawalAction(HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return ResponseEntity.status(501).body(null);

        member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return ResponseEntity.status(501).body(null);
        }

        commentService.deleteByMember(member);
        boardService.deleteByMember(member);

        followService.deleteByFollower(member);
        followService.deleteByFollowing(member);

        for (ChatRoomMember roomMember : roomMemberService.find(member)) {
            roomMember.getRoom().subCount();
        }

        messageService.deleteByMember(member);

        roomMemberService.delete(member);

        memberService.deleteById(member.getId());

        member = null;

        session.invalidate();

        return ResponseEntity.ok(member);
    }


    @GetMapping("/search/{name}/action")
    public ResponseEntity<Boolean> memberFindAction(@PathVariable String name, HttpSession session) {
        HashMap<String, Object> member_search = memberService.findName(name);

        session.setAttribute("member_search", member_search);

        return ResponseEntity.ok(member_search.isEmpty());
    }
}
