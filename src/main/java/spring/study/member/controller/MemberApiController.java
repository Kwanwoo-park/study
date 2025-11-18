package spring.study.member.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import spring.study.member.dto.MemberRequestDto;
import spring.study.member.dto.MemberResponseDto;
import spring.study.board.entity.Board;
import spring.study.chat.entity.ChatRoomMember;
import spring.study.forbidden.entity.Status;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.aws.service.ImageS3Service;
import spring.study.board.service.BoardImgService;
import spring.study.board.service.BoardService;
import spring.study.chat.service.ChatMessageService;
import spring.study.chat.service.ChatRoomMemberService;
import spring.study.comment.service.CommentService;
import spring.study.favorite.service.FavoriteService;
import spring.study.follow.service.FollowService;
import spring.study.forbidden.service.ForbiddenService;
import spring.study.member.service.MemberService;
import spring.study.member.service.UserService;
import spring.study.notification.service.NotificationService;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

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
    private final FavoriteService favoriteService;
    private final ChatRoomMemberService roomMemberService;
    private final ChatMessageService messageService;
    private final UserService userService;
    private final ForbiddenService forbiddenService;
    private final NotificationService notificationService;
    private final ImageS3Service imageS3Service;
    private final BCryptPasswordEncoder encoder;

    @PatchMapping("/login")
    public ResponseEntity<Member> loginAction(@RequestBody MemberRequestDto dto, HttpServletRequest request) {
        Member member = (Member) memberService.loadUserByUsername(dto.getEmail());
        HttpSession session = request.getSession();

        if (member == null)
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);

        if ( dto.getEmail().isEmpty() || dto.getEmail().isBlank() ||dto.getPassword().isEmpty() || dto.getPassword().isBlank() )
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);

        if (encoder.matches(dto.getPassword(), member.getPassword())) {
            if (member.getRole() != Role.DENIED) {
                memberService.updateLastLoginTime(member.getId());
                session.setAttribute("member", member);
            }

            return ResponseEntity.status(HttpStatus.OK).body(member);
        }

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(null);
    }

    @GetMapping("/logout")
    public ResponseEntity<Integer> logoutAction(HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);

        session.removeAttribute("member");

        return ResponseEntity.ok(1);
    }

    @PostMapping("/register")
    public ResponseEntity<Long> registerAction(@RequestBody MemberRequestDto memberRequestDto) throws Exception {
        if (memberRequestDto.getEmail().isEmpty() || memberRequestDto.getEmail().isBlank() ||
                memberRequestDto.getPassword().isEmpty() || memberRequestDto.getPassword().isBlank() ||
                memberRequestDto.getName().isEmpty() || memberRequestDto.getName().isBlank() ||
                memberRequestDto.getPhone().isEmpty() || memberRequestDto.getPhone().isBlank() ||
                memberRequestDto.getBirth().isEmpty() || memberRequestDto.getBirth().isBlank())
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(null);

        int risk = forbiddenService.findWordList(Status.APPROVAL, memberRequestDto.getName());

        if (risk != 0) {
            return ResponseEntity.ok(-1L);
        }

        notificationService.createNotification(memberService.findAdministrator(), memberRequestDto.getName() + "님이 회원가입 하였습니다");

        return ResponseEntity.ok(userService.createUser(memberRequestDto).getId());
    }

    @GetMapping("/duplicateCheck")
    public ResponseEntity<Integer> duplicateCheck(@RequestParam() String email) {
        if (email.isBlank())
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);

        return memberService.existEmail(email) ? ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(null) : ResponseEntity.status(HttpStatus.OK).body(null);
    }

    @PatchMapping("/detail/action")
    public ResponseEntity<Member> detailAction(@RequestPart MultipartFile file, HttpServletRequest request) throws IOException, FileNotFoundException {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        if (imageS3Service.fileFormatCheck(file))
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);

        imageS3Service.deleteImage(member.getProfile());

        String imageUrl = imageS3Service.uploadImageToS3(file);

        member.setProfile(imageUrl);
        memberService.updateProfile(member.getId(), imageUrl);

        session.setAttribute("member", member);

        return ResponseEntity.ok(member);
    }

    @GetMapping("/find/email")
    public ResponseEntity<Member> findAction(@RequestParam() String email) {
        if (email.isBlank())
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(null);

        Member member = memberService.findMember(email);

        return ResponseEntity.ok(member);
    }

    @GetMapping("/find/info")
    public ResponseEntity<Member> findAction(@RequestParam String birth, @RequestParam String phone) {
        if (birth.isBlank() || phone.isBlank())
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);

        phone = userService.replacePhoneNumber(phone);
        Member member = memberService.findMember(phone, birth);

        return ResponseEntity.ok(member);
    }

    @PatchMapping("/updatePassword")
    public ResponseEntity<Integer> updatePasswordAction(@RequestBody MemberRequestDto memberUpdateDto, HttpServletRequest request) {
        HttpSession session = request.getSession();

        Member member;

        if (session == null || !request.isRequestedSessionIdValid() || session.getAttribute("member") == null) {
            member = memberService.findMember(memberUpdateDto.getEmail());
        }
        else {
            member = (Member) session.getAttribute("member");
            session.invalidate();

            if (member == null)
                return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);
        }

        if (memberUpdateDto.getPassword().isEmpty() || memberUpdateDto.getPassword().isBlank())
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(null);

        int result = userService.updatePwd(member.getId(), memberUpdateDto.getPassword());

        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @PatchMapping("/updatePhone")
    public ResponseEntity<Integer> updatePhone(@RequestBody MemberRequestDto memberUpdateDto, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);

        if (memberUpdateDto.getEmail().isEmpty() || memberUpdateDto.getEmail().isBlank() ||
                memberUpdateDto.getPhone().isEmpty() || memberUpdateDto.getPhone().isBlank() ||
                memberUpdateDto.getBirth().isEmpty() || memberUpdateDto.getBirth().isBlank())
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(null);

        Member member = memberService.findMember(memberUpdateDto.getEmail());

        int result = memberService.updatePhoneAndBirth(member.getId(), memberUpdateDto.getPhone(), memberUpdateDto.getBirth());

        session.setAttribute("member", memberService.findMember(memberUpdateDto.getEmail()));

        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @GetMapping("/search")
    public ResponseEntity<List<MemberResponseDto>> searchMember(@RequestParam() String name, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);
        }

        return ResponseEntity.ok(memberService.findName(name));
    }

    @DeleteMapping("/withdrawal")
    public ResponseEntity<Boolean> withdrawalAction(@RequestParam(required = false) String email, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);
        }

        if (email == null) {
            for (Board board : member.getBoard()) {
                boardImgService.deleteBoard(board);
                favoriteService.deleteByBoard(board);
            }

            notificationService.deleteByMember(member);

            favoriteService.deleteByMember(member);
            commentService.deleteByMember(member);
            boardService.deleteByMember(member);

            followService.deleteByFollower(member);
            followService.deleteByFollowing(member);

            roomMemberService.subCount(member);

            messageService.deleteByMember(member);

            roomMemberService.delete(member);

            imageS3Service.deleteImage(member.getProfile());
            memberService.deleteById(member.getId());

            session.invalidate();
        }
        else {
            Member deleteMember = memberService.findMember(email);

            for (Board board : deleteMember.getBoard()) {
                boardImgService.deleteBoard(board);
                favoriteService.deleteByBoard(board);
            }

            notificationService.deleteByMember(deleteMember);

            favoriteService.deleteByMember(deleteMember);
            commentService.deleteByMember(deleteMember);
            boardService.deleteByMember(deleteMember);

            followService.deleteByFollower(deleteMember);
            followService.deleteByFollowing(deleteMember);

            roomMemberService.subCount(member);

            messageService.deleteByMember(deleteMember);

            roomMemberService.delete(deleteMember);

            imageS3Service.deleteImage(deleteMember.getProfile());
            memberService.deleteById(deleteMember.getId());
        }

        return ResponseEntity.ok(true);
    }
}
