package spring.study.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import spring.study.dto.member.MemberRequestDto;
import spring.study.dto.member.MemberResponseDto;
import spring.study.entity.Board;
import spring.study.entity.Comment;
import spring.study.entity.Member;
import spring.study.service.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/member")
@Slf4j
public class MemberApiController {
    private final MemberService memberService;
    private final BoardService boardService;
    private final CommentService commentService;
    private final FollowService followService;
    private final UserService userService;
    private Member member;

    @PostMapping("/register/action")
    public ResponseEntity<MemberResponseDto> registerAction(@RequestBody MemberRequestDto memberRequestDto) throws Exception {
        return ResponseEntity.ok(userService.createUser(memberRequestDto));
    }

    @PatchMapping("/detail/action")
    public ResponseEntity<Member> detailAction(@RequestParam MultipartFile file, HttpSession session) throws IOException {
        String fileDir = "/Users/lg/Desktop/study/study/src/main/resources/static/img/";

        member = (Member) session.getAttribute("member");

        File f = new File(fileDir + file.getOriginalFilename());

        if (!f.exists()) {
            file.transferTo(new File(fileDir + file.getOriginalFilename()));
        }

        member.setProfile(file.getOriginalFilename());
        memberService.updateProfile(member.getId(), file.getOriginalFilename());

        session.setAttribute("member", member);

        return ResponseEntity.ok(member);
    }

    @GetMapping("/find/email/{email}/action")
    public ResponseEntity<Member> findAction(@PathVariable String email) {
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
        }

        int result = userService.updatePwd(member.getId(), memberUpdateDto.getPassword());

        member = null;

        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/withdrawal/action")
    public ResponseEntity<Member> withdrawalAction(HttpSession session) {
        member = (Member) session.getAttribute("member");

        commentService.deleteByMember(member);
        boardService.deleteByMember(member);

        followService.deleteByFollower(member);
        followService.deleteByFollowing(member);

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
