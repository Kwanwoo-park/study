package spring.study.member.facade;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import spring.study.aws.service.ImageS3Service;
import spring.study.board.entity.Board;
import spring.study.board.service.BoardImgService;
import spring.study.board.service.BoardService;
import spring.study.chat.service.ChatMessageService;
import spring.study.chat.service.ChatRoomMemberService;
import spring.study.comment.service.CommentService;
import spring.study.common.service.SessionManager;
import spring.study.favorite.service.FavoriteService;
import spring.study.follow.service.FollowService;
import spring.study.forbidden.entity.Status;
import spring.study.forbidden.service.ForbiddenService;
import spring.study.member.dto.MemberRequestDto;
import spring.study.member.dto.MemberResponseDto;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.service.MemberService;
import spring.study.member.service.UserService;
import spring.study.notification.entity.Group;
import spring.study.notification.service.NotificationService;
import spring.study.reply.service.ReplyService;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberFacade {
    private final SessionManager sessionManager;
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

        sessionManager.setLoginMember(request, memberService.getIp(request), member);

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

    public ResponseEntity<?> changeProfileImage(MultipartFile file, Member member, HttpServletRequest request) {
        if (imageS3Service.fileFormatCheck(file)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -10L,
                    "message", "지원하지 않는 파일 포맷입니다"
            ));
        }

        imageS3Service.deleteImage(member.getProfile());

        try {
            String imageUrl = imageS3Service.uploadImageToS3(file);

            member.setProfile(imageUrl);
            memberService.updateProfile(member.getId(), imageUrl);

            request.getSession(false).setAttribute("member", member);

            return ResponseEntity.ok(Map.of(
                    "result", member.getId()
            ));
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "result", -10L,
                    "message", "이미지 업로드 중 오류가 발생하였습니다"
            ));
        }
    }

    public ResponseEntity<?> findEmail(String email) {
        if (email.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -10L,
                    "message", "이메일이 없습니다"
            ));
        }

        Member member = memberService.findMember(email);

        if (member == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "result", -10L,
                    "message", "존재하지 않는 회원 이메일입니다"
            ));
        } else {
            return ResponseEntity.ok(Map.of(
                    "result", member.getId(),
                    "member", member
            ));
        }
    }

    public ResponseEntity<?> findInfo(String birth, String phone) {
        int check = validateBirthAndPhone(birth, phone);

        if (check == -1) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -10L,
                    "message", "생년월일이 입력되지 않았습니다"
            ));
        } else if (check == -2) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -10L,
                    "message", "전화번호가 입력되지 않았습니다"
            ));
        }

        phone = userService.replacePhoneNumber(phone);
        Member member = memberService.findMember(phone, birth);

        if (member == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "result", -10L,
                    "message", "존재하지 않는 회원 정보입니다"
            ));
        } else {
            return ResponseEntity.ok(Map.of(
                    "result", member.getId(),
                    "member", member
            ));
        }
    }

    public ResponseEntity<?> updatePassword(String password, Member member) {
        if (password == null || password.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -10L,
                    "message", "비밀번호가 입력되지 않았습니다"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "result", userService.updatePwd(member.getId(), password)
        ));
    }

    public ResponseEntity<?> updatePhone(MemberRequestDto dto, HttpServletRequest request) {
        int check = validateMember2(dto);

        if (check == -1) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -10L,
                    "message", "이메일이 입력되지 않았습니다"
            ));
        } else if (check == -2) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -10L,
                    "message", "전화번호가 입력되지 않았습니다"
            ));
        } else if (check == -3) {
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

        Member member = memberService.findMember(dto.getEmail());

        int result = memberService.updatePhoneAndBirth(member.getId(), dto.getPhone(), dto.getBirth());

        if (result != -2) {
            request.getSession(false).setAttribute("member", member);

            return ResponseEntity.ok(Map.of(
                    "result", member.getId()
            ));
        } else {
            return ResponseEntity.status(HttpStatus.FOUND).body(Map.of(
                    "result", -2L,
                    "message", "이미 존재하는 전화번호입니다"
            ));
        }
    }

    public ResponseEntity<?> search(String name) {
        return ResponseEntity.ok(Map.of(
                "result", 10L,
                "list", memberService.findName(name)
        ));
    }

    public ResponseEntity<?> deleteMember(Member member, HttpServletRequest request) {
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

        request.getSession(false).invalidate();

        return ResponseEntity.ok(Map.of(
                "result", member.getId()
        ));
    }

    public ResponseEntity<?> deleteMember(String email) {
        Member member = memberService.findMember(email);

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

        return ResponseEntity.ok(Map.of(
                "result", member.getId()
        ));
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

    private int validateBirthAndPhone(String birth, String phone) {
        if (birth == null || birth.isBlank()) return -1;
        else if (phone == null || phone.isBlank()) return -2;
        else return 0;
    }

    private int validateMember2(MemberRequestDto dto) {
        if (dto.getEmail() == null || dto.getEmail().isBlank()) return -1;
        else if (dto.getPhone() == null || dto.getPhone().isBlank()) return -2;
        else if (dto.getBirth() == null || dto.getBirth().isBlank()) return -3;
        else return 0;
    }
}
