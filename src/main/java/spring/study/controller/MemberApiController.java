package spring.study.controller;

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
import spring.study.service.BoardService;
import spring.study.service.CommentService;
import spring.study.service.MemberService;
import spring.study.service.UserService;

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

    @GetMapping("/find/{email}/action")
    public ResponseEntity<Member> findAction(@PathVariable String email, HttpSession session) {
        member = memberService.findMember(email);

        session.setAttribute("member", member);

        return ResponseEntity.ok(member);
    }

    @PatchMapping("/updatePassword/action")
    public ResponseEntity<Integer> updatePasswordAction(@RequestBody MemberRequestDto memberUpdateDto) {
        member = memberService.findMember(memberUpdateDto.getEmail());
        int result = userService.updatePwd(member.getId(), memberUpdateDto.getPassword());

        member = null;

        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/withdrawal/action")
    public ResponseEntity<Member> withdrawalAction(HttpSession session) {
        member = (Member) session.getAttribute("member");

        List<Board> list = member.getBoard();
        if (list.size() > 0) {
            for (Board b : list) {
                commentService.deleteComment(b);
                boardService.deleteById(b.getId());
            }
        }

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
