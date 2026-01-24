package spring.study.member.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
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
import spring.study.notification.entity.Group;
import spring.study.notification.service.NotificationService;
import spring.study.reply.service.ReplyService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/member")
@Slf4j
public class MemberApiController {
    private final MemberService memberService;
    private final BoardService boardService;
    private final BoardImgService boardImgService;
    private final CommentService commentService;
    private final ReplyService replyService;
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
    public ResponseEntity<Map<String, Object>> loginAction(@RequestBody MemberRequestDto dto, HttpServletRequest request) {

        Map<String, Object> map = new HashMap<>();
        HttpSession session = request.getSession();

        try {
            Member member = (Member) memberService.loadUserByUsername(dto.getEmail());

            if (member == null) {
                map.put("result", -3L);
                return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
            }

            if ( dto.getEmail().isEmpty() || dto.getEmail().isBlank() ||dto.getPassword().isEmpty() || dto.getPassword().isBlank()) {
                map.put("result", -10L);
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(map);
            }

            if (encoder.matches(dto.getPassword(), member.getPassword())) {
                if (member.getRole() != Role.DENIED) {
                    memberService.updateLastLoginTime(member.getId());

                    session.setAttribute("IP", memberService.getIp(request));
                    session.setAttribute("UA", request.getHeader("User-Agent"));
                    session.setAttribute("member", member);

                    map.put("result", member.getId());
                    map.put("member", member);
                } else
                    map.put("result", -2L);
            } else
                map.put("result", -1L);

            return ResponseEntity.status(HttpStatus.OK).body(map);
        } catch (Exception e) {
            log.error(e.getMessage());
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(map);
        }
    }

    @GetMapping("/logout")
    public ResponseEntity<Map<String, Long>> logoutAction(HttpServletRequest request) {
        HttpSession session = request.getSession();
        Map<String, Long> map = new HashMap<>();

        if (session == null || !request.isRequestedSessionIdValid()) {
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        if (session.getAttribute("member") == null) {
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        if (!memberService.validateSession(request)) {
            session.invalidate();
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        Member member = (Member) session.getAttribute("member");

        try {
            session.removeAttribute("member");

            map.put("result", member.getId());

            return ResponseEntity.ok(map);
        } catch (Exception e) {
            log.error(e.getMessage());
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Long>> registerAction(@RequestBody @Valid MemberRequestDto memberRequestDto) throws Exception {
        Map<String, Long> map = new HashMap<>();

        if ((memberRequestDto.getEmail().isEmpty() || memberRequestDto.getEmail().isBlank()) ||
                (memberRequestDto.getPassword().isEmpty() || memberRequestDto.getPassword().isBlank()) ||
                (memberRequestDto.getName().isEmpty() || memberRequestDto.getName().isBlank()) ||
                (memberRequestDto.getPhone().isEmpty() || memberRequestDto.getPhone().isBlank()) ||
                (memberRequestDto.getBirth().isEmpty() || memberRequestDto.getBirth().isBlank())) {
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(map);
        }

        if (memberRequestDto.getBirth().equals("1900-01-01")) {
            map.put("result", -2L);
            return ResponseEntity.ok(map);
        }

        try {
            int risk = forbiddenService.findWordList(Status.APPROVAL, memberRequestDto.getName());

            if (risk != 0) {
                map.put("result", -1L);
                return ResponseEntity.ok(map);
            }

            MemberResponseDto dto = userService.createUser(memberRequestDto);

            if (dto != null) {
                notificationService.createNotification(memberService.findAdministrator(), memberRequestDto.getName() + "님이 회원가입 하였습니다", Group.ADMIN);
                map.put("result", dto.getId());
            }
            else
                map.put("result", -2L);

            return ResponseEntity.ok(map);
        } catch (Exception e) {
            log.error(e.getMessage());
            map.put("result", -2L);
            return ResponseEntity.ok(map);
        }
    }

    @GetMapping("/duplicateCheck")
    public ResponseEntity<Map<String, Long>> duplicateCheck(@RequestParam() String email) {
        Map<String, Long> map = new HashMap<>();

        if (email.isBlank()) {
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        if (memberService.existEmail(email))
            map.put("result", 10L);
        else
            map.put("result", -10L);

        return ResponseEntity.ok(map);
    }

    @PatchMapping("/detail/action")
    public ResponseEntity<Map<String, Long>> detailAction(@RequestPart MultipartFile file, HttpServletRequest request) {
        HttpSession session = request.getSession();
        Map<String, Long> map = new HashMap<>();

        if (session == null || !request.isRequestedSessionIdValid()) {
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
        }

        if (!memberService.validateSession(request)) {
            session.invalidate();
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        if (imageS3Service.fileFormatCheck(file)) {
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
        }

        try {
            imageS3Service.deleteImage(member.getProfile());

            String imageUrl = imageS3Service.uploadImageToS3(file);

            member.setProfile(imageUrl);
            memberService.updateProfile(member.getId(), imageUrl);

            session.setAttribute("member", member);

            map.put("result", member.getId());

            return ResponseEntity.ok(map);
        } catch (Exception e) {
            log.error(e.getMessage());
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
        }
    }

    @GetMapping("/find/email")
    public ResponseEntity<Map<String, Object>> findAction(@RequestParam() String email) {
        Map<String, Object> map = new HashMap<>();

        if (email.isBlank()) {
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(map);
        }

        try {
            Member member = memberService.findMember(email);
            map.put("result", member.getId());
            map.put("member", member);

            return ResponseEntity.ok(map);
        } catch (Exception e) {
            log.error(e.getMessage());
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
        }
    }

    @GetMapping("/find/info")
    public ResponseEntity<Map<String, Object>> findAction(@RequestParam String birth, @RequestParam String phone) {
        Map<String, Object> map = new HashMap<>();

        if (birth.isBlank() || phone.isBlank()) {
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        try {
            phone = userService.replacePhoneNumber(phone);
            Member member = memberService.findMember(phone, birth);

            map.put("result", member.getId());
            map.put("member", member);

            return ResponseEntity.ok(map);
        } catch (Exception e) {
            log.error(e.getMessage());
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
        }
    }

    @PatchMapping("/updatePassword")
    public ResponseEntity<Map<String, Long>> updatePasswordAction(@RequestBody MemberRequestDto memberUpdateDto, HttpServletRequest request) {
        HttpSession session = request.getSession();
        Map<String, Long> map = new HashMap<>();
        Member member;

        if (session == null || !request.isRequestedSessionIdValid() || session.getAttribute("member") == null) {
            member = memberService.findMember(memberUpdateDto.getEmail());
        } else if (!memberService.validateSession(request)) {
            member = memberService.findMember(memberUpdateDto.getEmail());
        }
        else {
            member = (Member) session.getAttribute("member");
            session.invalidate();

            if (member == null) {
                map.put("result", -10L);
                return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
            }
        }

        if (memberUpdateDto.getPassword().isEmpty() || memberUpdateDto.getPassword().isBlank()) {
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(map);
        }

        try {
            map.put("result", userService.updatePwd(member.getId(), memberUpdateDto.getPassword()));

            return ResponseEntity.ok(map);
        } catch (Exception e) {
            log.error(e.getMessage());
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
        }
    }

    @PatchMapping("/updatePhone")
    public ResponseEntity<Map<String, Long>> updatePhone(@RequestBody @Valid MemberRequestDto memberUpdateDto, HttpServletRequest request) {
        Map<String, Long> map = new HashMap<>();
        HttpSession session = request.getSession();

        if (memberUpdateDto.getEmail().isEmpty() || memberUpdateDto.getEmail().isBlank() ||
                memberUpdateDto.getPhone().isEmpty() || memberUpdateDto.getPhone().isBlank() ||
                memberUpdateDto.getBirth().isEmpty() || memberUpdateDto.getBirth().isBlank()) {
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(map);
        }

        if (memberUpdateDto.getBirth().equals("1900-01-01")) {
            map.put("result", -2L);
            return ResponseEntity.ok(map);
        }

        try {
            Member member = memberService.findMember(memberUpdateDto.getEmail());

            int result = memberService.updatePhoneAndBirth(member.getId(), memberUpdateDto.getPhone(), memberUpdateDto.getBirth());

            if (result != -2) {
                session.setAttribute("member", memberService.findMember(memberUpdateDto.getEmail()));
                map.put("result", member.getId());
            }
            else
                map.put("result", -2L);

            return ResponseEntity.ok(map);
        } catch (Exception e) {
            log.error(e.getMessage());
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchMember(@RequestParam() String name, HttpServletRequest request) {
        HttpSession session = request.getSession();
        Map<String, Object> map = new HashMap<>();

        if (session == null || !request.isRequestedSessionIdValid()) {
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        if (!memberService.validateSession(request)) {
            session.invalidate();
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        try {
            List<MemberResponseDto> list = memberService.findName(name);

            map.put("result", 10L);
            map.put("list", list);

            return ResponseEntity.ok(map);
        } catch (Exception e) {
            log.error(e.getMessage());
            map.put("result", -10L);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
        }
    }

    @DeleteMapping("/withdrawal")
    public ResponseEntity<Map<String, Long>> withdrawalAction(@RequestParam(required = false) String email, HttpServletRequest request) {
        HttpSession session = request.getSession();
        Map<String, Long> map = new HashMap<>();

        if (session == null || !request.isRequestedSessionIdValid()) {
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        if (!memberService.validateSession(request)) {
            session.invalidate();
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        try {
            if (email == null) {
                map.put("result", member.getId());

                for (Board board : member.getBoard()) {
                    boardImgService.deleteBoard(board);
                    favoriteService.deleteByBoard(board);
                }

                notificationService.deleteByMember(member);

                favoriteService.deleteByMember(member);
                replyService.deleteReplay(member.getComment());
                replyService.deleteReply(member);
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

                map.put("result", deleteMember.getId());

                for (Board board : deleteMember.getBoard()) {
                    boardImgService.deleteBoard(board);
                    favoriteService.deleteByBoard(board);
                }

                notificationService.deleteByMember(deleteMember);

                favoriteService.deleteByMember(deleteMember);
                replyService.deleteReplay(member.getComment());
                replyService.deleteReply(member);
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

            return ResponseEntity.ok(map);
        } catch (Exception e) {
            log.error(e.getMessage());
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
        }
    }
}
