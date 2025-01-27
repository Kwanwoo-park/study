package spring.study.controller.member;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import spring.study.dto.member.MemberRequestDto;
import spring.study.dto.member.MemberResponseDto;
import spring.study.entity.board.Board;
import spring.study.entity.chat.ChatRoomMember;
import spring.study.entity.member.Member;
import spring.study.entity.member.Role;
import spring.study.service.board.BoardImgService;
import spring.study.service.board.BoardService;
import spring.study.service.chat.ChatMessageService;
import spring.study.service.chat.ChatRoomMemberService;
import spring.study.service.comment.CommentService;
import spring.study.service.follow.FollowService;
import spring.study.service.member.MemberService;
import spring.study.service.member.UserService;

import java.io.File;
import java.io.IOException;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/member")
@Slf4j
public class MemberApiController {
    private final MemberService memberService;
    private final BoardService boardService;
    private final BoardImgService boardImgService;
    private final CommentService commentService;
    private final FollowService followService;
    private final ChatRoomMemberService roomMemberService;
    private final ChatMessageService messageService;
    private final UserService userService;
    private Member member;

    @PostMapping("/login")
    public ResponseEntity<Member> loginAction(@RequestBody MemberRequestDto dto, HttpServletRequest request) {
        member = (Member) memberService.loadUserByUsername(dto.getEmail());

        if (member == null)
            return ResponseEntity.status(501).body(null);

        if (new BCryptPasswordEncoder().matches(dto.getPassword(), member.getPassword())) {
            if (member.getRole() != Role.DENIED) {
                memberService.updateLastLoginTime(member.getId());
                request.getSession().setAttribute("member", member);
            }

            return ResponseEntity.status(HttpStatus.OK).body(member);
        }

        return ResponseEntity.status(501).body(null);
    }

    @PostMapping("/register")
    public ResponseEntity<MemberResponseDto> registerAction(@RequestBody MemberRequestDto memberRequestDto) throws Exception {
        return ResponseEntity.ok(userService.createUser(memberRequestDto));
    }

    @GetMapping("/duplicateCheck")
    public ResponseEntity<Integer> duplicateCheck(@RequestParam() String email) {
        return memberService.existEmail(email) ? ResponseEntity.status(501).body(null) : ResponseEntity.status(200).body(null);
    }

    @PatchMapping("/detail/action")
    public ResponseEntity<Member> detailAction(@RequestPart MultipartFile file, HttpServletRequest request) throws IOException {
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

        member.setProfile(file.getOriginalFilename());
        memberService.updateProfile(member.getId(), file.getOriginalFilename());

        session.setAttribute("member", member);

        return ResponseEntity.ok(member);
    }

    @GetMapping("/find/email")
    public ResponseEntity<Member> findAction(@RequestParam() String email) {
        member = memberService.findMember(email);

        return ResponseEntity.ok(member);
    }

    @GetMapping("/find/info")
    public ResponseEntity<Member> findAction(@RequestParam String birth, @RequestParam String phone) {
        String regEx = "(\\d{3})(\\d{3,4})(\\d{4})";
        phone = phone.replaceAll(regEx, "$1-$2-$3");
        member = memberService.findMember(phone, birth);

        return ResponseEntity.ok(member);
    }

    @PatchMapping("/updatePassword")
    public ResponseEntity<Integer> updatePasswordAction(@RequestBody MemberRequestDto memberUpdateDto, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid() || session.getAttribute("member") == null) {
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

        return ResponseEntity.status(200).body(result);
    }

    @PatchMapping("/updatePhone")
    public ResponseEntity<Integer> updatePhone(@RequestBody MemberRequestDto memberUpdateDto, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return ResponseEntity.status(501).body(null);

        member = memberService.findMember(memberUpdateDto.getEmail());

        int result = memberService.updatePhone(member.getId(), memberUpdateDto.getPhone());

        session.setAttribute("member", memberService.findMember(memberUpdateDto.getEmail()));

        return ResponseEntity.status(200).body(result);
    }

    @DeleteMapping("/withdrawal")
    public ResponseEntity<Boolean> withdrawalAction(HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return ResponseEntity.status(501).body(null);

        member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return ResponseEntity.status(501).body(null);
        }

        for (Board board : member.getBoard()) {
            boardImgService.deleteBoard(board);
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

        return ResponseEntity.ok(true);
    }
}
