package spring.study.member.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import spring.study.aws.service.ImageS3Service;
import spring.study.board.service.BoardImgService;
import spring.study.board.service.BoardService;
import spring.study.chat.service.ChatMessageService;
import spring.study.chat.service.ChatRoomMemberService;
import spring.study.comment.service.CommentService;
import spring.study.common.service.SessionService;
import spring.study.favorite.service.FavoriteService;
import spring.study.follow.service.FollowService;
import spring.study.forbidden.entity.Status;
import spring.study.forbidden.service.ForbiddenService;
import spring.study.member.dto.MemberRequestDto;
import spring.study.member.dto.MemberResponseDto;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.notification.entity.Group;
import spring.study.notification.service.NotificationService;
import spring.study.reply.service.ReplyService;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberFacade {
    private final SessionService sessionService;
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

    public ResponseEntity<?> login(MemberRequestDto dto, HttpServletRequest request) {
        int check = validateLogin(dto);

        if (check == -1) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -10L,
                    "message", "이메일이 입력되지 않았습니다"
            ));
        } else if (check == -2) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -10L,
                    "message", "비밀번호가 입력되지 않았습니다"
            ));
        }

        Member member = (Member) memberService.loadUserByUsername(dto.getEmail());

        if (member == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -3L,
                    "message", "존재하지 않는 회원입니다"
            ));
        }

        if (!encoder.matches(dto.getPassword(), member.getPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -1L,
                    "message", "잘못된 비밀번호 입니다"
            ));
        }

        if (member.getRole() == Role.DENIED) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -2L,
                    "message", "차단된 계정입니다"
            ));
        }

        memberService.updateLastLoginTime(member.getId());

        sessionService.setLoginMember(request, memberService.getIp(request), member);

        return ResponseEntity.ok(Map.of(
                "result", member.getId(),
                "member", member
        ));
    }

    public ResponseEntity<?> register(MemberRequestDto dto) {
        int check = validateMember(dto);

        if (check == -1) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -10L,
                    "message", "이메일이 입력되지 않았습니다"
            ));
        } else if (check == -2) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -10L,
                    "message", "비밀번호가 입력되지 않았습니다"
            ));
        } else if (check == -3) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -10L,
                    "message", "이름이 입력되지 않았습니다"
            ));
        } else if (check == -4) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -10L,
                    "message", "전화번호가 입력되지 않았습니다"
            ));
        } else if (check == -5) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -10L,
                    "message", "생년월일이 입력되지 않았습니다"
            ));
        }

        if (dto.getBirth().equals("1900-01-01")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -2L,
                    "message", "유효하지 않은 생년월일 입니다"
            ));
        }

        check = validateContent(dto.getName());

        if (check != 0) {
            if (check == -99) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "result", check,
                        "message", "이름이 입력되지 않았습니다"
                ));
            }

            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of(
                    "result", -1L,
                    "message", "금칙어를 사용하였습니다"
            ));
        }

        MemberResponseDto response = userService.createUser(dto);

        if (response != null) {
            notificationService.createNotification(memberService.findAdministrator(), dto.getName() + "님이 회원가입 하였습니다", Group.ADMIN);

            return ResponseEntity.ok(Map.of(
                    "result", dto.getId()
            ));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "result", -2L,
                    "message", "회원가입에 실패하였습니다"
            ));
        }
    }

    public ResponseEntity<?> duplicateCheck(String email) {
        if (email.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -10L,
                    "message", "이메일이 없습니다"
            ));
        }

        if (!memberService.existEmail(email)) {
            return ResponseEntity.ok(Map.of(
                    "result", 10L
            ));
        } else {
            return ResponseEntity.status(HttpStatus.FOUND).body(Map.of(
                    "result", -10L,
                    "message", "이미 존재하는 이메일입니다"
            ));
        }
    }

    private int validateLogin(MemberRequestDto dto) {
        if (dto.getEmail() == null || dto.getEmail().isBlank()) return -1;
        else if (dto.getPassword() == null || dto.getPassword().isBlank()) return -2;
        else return 0;
    }

    private int validateMember(MemberRequestDto dto) {
        if (dto.getEmail() == null || dto.getEmail().isBlank()) return -1;
        else if (dto.getPassword() == null || dto.getPassword().isBlank()) return -2;
        else if (dto.getName() == null || dto.getName().isBlank()) return -3;
        else if (dto.getPhone() == null || dto.getPhone().isBlank()) return -4;
        else if (dto.getBirth() == null || dto.getBirth().isBlank()) return -5;
        else return 0;
    }

    private int validateContent(String name) {
        if (name == null || name.isBlank()) {
            return -99;
        }

        return forbiddenService.findWordList(Status.APPROVAL, name);
    }
}
