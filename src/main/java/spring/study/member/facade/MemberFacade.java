package spring.study.member.facade;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import spring.study.account.service.AccountService;
import spring.study.memo.service.MemoService;
import spring.study.appeal.service.AppealService;
import spring.study.aws.service.ImageS3Service;
import spring.study.board.entity.Board;
import spring.study.board.service.BoardImgService;
import spring.study.board.service.BoardService;
import spring.study.bookreview.service.BookReviewService;
import spring.study.chat.service.ChatMessageService;
import spring.study.chat.service.ChatRoomMemberService;
import spring.study.collection.entity.Collection;
import spring.study.collection.service.CollectionService;
import spring.study.comment.service.CommentService;
import spring.study.common.entity.CommonVisibility;
import spring.study.common.service.VisibilityAccessPolicy;
import spring.study.diary.entity.Diary;
import spring.study.diary.entity.DiaryImage;
import spring.study.favorite.service.FavoriteService;
import spring.study.follow.service.FollowService;
import spring.study.forbidden.entity.Status;
import spring.study.forbidden.service.ForbiddenService;
import spring.study.member.dto.MemberRequestDto;
import spring.study.member.dto.MemberResponseDto;
import spring.study.member.entity.Member;
import spring.study.jwt.component.JwtTokenProvider;
import spring.study.jwt.dto.IssuedToken;
import spring.study.jwt.dto.TokenClaims;
import spring.study.jwt.entity.RefreshToken;
import spring.study.jwt.entity.TokenName;
import spring.study.jwt.service.JwtCookieService;
import spring.study.jwt.service.MemberTokenCacheService;
import spring.study.jwt.service.RefreshTokenService;
import spring.study.common.exception.JwtValidationException;
import spring.study.common.component.TransactionRunner;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import java.time.Instant;
import spring.study.member.service.MemberService;
import spring.study.member.service.UserService;
import spring.study.notification.entity.Group;
import spring.study.notification.service.NotificationService;
import spring.study.report.service.ReportService;
import spring.study.reply.service.ReplyService;
import spring.study.todo.service.TodoService;
import spring.study.mail.service.EmailVerificationService;
import spring.study.mail.entity.EmailVerificationPurpose;
import spring.study.diary.service.DiaryService;
import spring.study.diary.service.DiaryImageService;
import spring.study.sanction.service.MemberSanctionService;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberFacade {
    private final MemberService memberService;
    private final VisibilityAccessPolicy visibilityAccessPolicy;
    private final BoardService boardService;
    private final BoardImgService boardImgService;
    private final CommentService commentService;
    private final ReplyService replyService;
    private final FollowService followService;
    private final FavoriteService favoriteService;
    private final ChatRoomMemberService roomMemberService;
    private final ChatMessageService messageService;
    private final CollectionService collectionService;
    private final AccountService accountService;
    private final UserService userService;
    private final ForbiddenService forbiddenService;
    private final NotificationService notificationService;
    private final ReportService reportService;
    private final AppealService appealService;
    private final BookReviewService bookReviewService;
    private final ImageS3Service imageS3Service;
    private final BCryptPasswordEncoder encoder;
    private final JwtTokenProvider tokenProvider;
    private final MemberTokenCacheService memberTokenCacheService;
    private final RefreshTokenService refreshTokenService;
    private final JwtCookieService cookieService;
    private final TransactionRunner transactions;
    private final TodoService todoService;
    private final MemoService memoService;
    private final EmailVerificationService emailVerificationService;
    private final DiaryService diaryService;
    private final DiaryImageService diaryImageService;
    private final MemberSanctionService memberSanctionService;

    public ResponseEntity<?> login(MemberRequestDto dto, HttpServletResponse response) {
        return login(dto, response, null);
    }

    public ResponseEntity<?> login(MemberRequestDto dto, HttpServletResponse response, String ipAddress) {
        return login(dto, null, response, ipAddress);
    }

    public ResponseEntity<?> login(MemberRequestDto dto, HttpServletRequest request, HttpServletResponse response, String ipAddress) {
        if (!StringUtils.hasText(dto.getEmail())) return invalidInput("이메일이 입력되지 않았습니다");
        if (!StringUtils.hasText(dto.getPassword())) return invalidInput("비밀번호가 입력되지 않았습니다");

        Member member = (Member) memberService.loadUserByUsername(dto.getEmail());

        if (!encoder.matches(dto.getPassword(), member.getPassword())) {
            return failure(HttpStatus.BAD_REQUEST, -1L, "잘못된 비밀번호 입니다");
        }

        if (member.isAccessBlocked()) {
            return failure(HttpStatus.BAD_REQUEST, -2L, "차단된 계정입니다");
        }

        member = memberService.updateLastLoginTime(member.getId());
        establishSession(member, request, response, ipAddress);
        return memberResponse(member);
    }

    @Transactional
    public ResponseEntity<?> register(MemberRequestDto dto) {
        if (!StringUtils.hasText(dto.getEmail())) return invalidInput("이메일이 입력되지 않았습니다");
        if (!StringUtils.hasText(dto.getPassword())) return invalidInput("비밀번호가 입력되지 않았습니다");
        if (!StringUtils.hasText(dto.getName())) return invalidInput("이름이 입력되지 않았습니다");
        if (!StringUtils.hasText(dto.getPhone())) return invalidInput("전화번호가 입력되지 않았습니다");
        if (!StringUtils.hasText(dto.getBirth())) return invalidInput("생년월일이 입력되지 않았습니다");

        if (dto.getBirth().equals("1900-01-01")) {
            return failure(HttpStatus.BAD_REQUEST, -2L, "유효하지 않은 생년월일 입니다");
        }

        if (forbiddenService.findWordList(Status.APPROVAL, dto.getName()) != 0) {
            return failure(HttpStatus.UNPROCESSABLE_ENTITY, -1L, "금칙어를 사용하였습니다");
        }

        if (!emailVerificationService.consume(EmailVerificationPurpose.EMAIL, dto.getEmail(), dto.getVerificationToken())) {
            return failure(HttpStatus.FORBIDDEN, -403L, "5분 이내에 이메일 인증을 완료한 후 가입해 주세요");
        }

        MemberResponseDto response = userService.createUser(dto);
        if (response == null) {
            return failure(HttpStatus.INTERNAL_SERVER_ERROR, -2L, "회원가입에 실패하였습니다");
        }

        notificationService.createNotification(memberService.findAdministrator(), dto.getName() + "님이 회원가입 하였습니다", Group.ADMIN);
        return ResponseEntity.ok(Map.of("result", response.getId()));
    }

    public ResponseEntity<?> duplicateCheck(String email) {
        if (email.isBlank()) return invalidInput("이메일이 없습니다");
        if (memberService.existEmail(email)) {
            return failure(HttpStatus.CONFLICT, -10L, "이미 존재하는 이메일입니다");
        }
        return ResponseEntity.ok(Map.of("result", 10L));
    }

    public ResponseEntity<?> changeProfileImage(MultipartFile file, Member member) {
        if (imageS3Service.fileFormatCheck(file)) {
            return invalidInput("지원하지 않는 파일 포맷입니다");
        }

        String imageUrl = "", oldProfile;

        try {
            imageUrl = imageS3Service.uploadImageToS3(file);
            oldProfile = member.getProfile();

            member.setProfile(imageUrl);
            memberService.updateProfile(member.getId(), imageUrl);
        } catch (Exception e) {
            imageS3Service.deleteImage(imageUrl);
            log.error("profile image change failed", e);
            return failure(HttpStatus.INTERNAL_SERVER_ERROR, -10L, "이미지 업로드 중 오류가 발생하였습니다");
        }

        imageS3Service.deleteImage(oldProfile);

        return ResponseEntity.ok(Map.of("result", member.getId()));
    }

    public ResponseEntity<?> findEmail(String email) {
        if (email.isBlank()) return invalidInput("이메일이 없습니다");
        return memberResponse(memberService.findMember(email));
    }

    public ResponseEntity<?> findInfo(String birth, String phone) {
        if (!StringUtils.hasText(birth)) return invalidInput("생년월일이 입력되지 않았습니다");
        if (!StringUtils.hasText(phone)) return invalidInput("전화번호가 입력되지 않았습니다");

        Member member = memberService.findMember(userService.replacePhoneNumber(phone), birth);

        if (member == null) {
            return failure(HttpStatus.NOT_FOUND, -10L, "존재하지 않는 회원 정보입니다");
        }
        return memberResponse(member);
    }

    public ResponseEntity<?> updatePassword(String password, Member member) {
        if (!StringUtils.hasText(password)) return invalidInput("비밀번호가 입력되지 않았습니다");

        return ResponseEntity.ok(Map.of("result", userService.updatePwd(member.getId(), password)));
    }

    public ResponseEntity<?> updateVisibility(CommonVisibility visibility, Member member) {
        if (visibility == null) {
            return failure(HttpStatus.BAD_REQUEST, -1L, "공개 설정을 선택해 주세요.");
        }

        long memberId = memberService.updateVisibility(member.getId(), visibility);

        return ResponseEntity.ok(Map.of(
                "result", memberId,
                "visibility", visibility
        ));
    }

    public ResponseEntity<?> updatePhone(MemberRequestDto dto) {
        if (!StringUtils.hasText(dto.getEmail())) return invalidInput("이메일이 입력되지 않았습니다");
        if (!StringUtils.hasText(dto.getPhone())) return invalidInput("전화번호가 입력되지 않았습니다");
        if (!StringUtils.hasText(dto.getBirth())) return invalidInput("생년월일이 입력되지 않았습니다");

        if (dto.getBirth().equals("1900-01-01")) {
            return failure(HttpStatus.BAD_REQUEST, -2L, "유효하지 않은 생년월일 입니다");
        }

        Member member = memberService.findMember(dto.getEmail());
        int result = memberService.updatePhoneAndBirth(member.getId(), dto.getPhone(), dto.getBirth());

        if (result == -2) {
            return failure(HttpStatus.CONFLICT, -2L, "이미 존재하는 전화번호입니다");
        }
        return ResponseEntity.ok(Map.of("result", member.getId()));
    }

    public ResponseEntity<?> search(String name, Member viewer) {
        if (name == null || name.isBlank()) {
            return ResponseEntity.ok(Map.of(
                    "result", 10L,
                    "list", List.of()
            ));
        }

        return ResponseEntity.ok(Map.of(
                "result", 10L,
                "list", memberService.findMembersByName(name.trim()).stream()
                        .filter(member -> visibilityAccessPolicy.canViewMember(member, viewer))
                        .map(MemberResponseDto::new).toList()
        ));
    }

    @Transactional
    public ResponseEntity<?> deleteMember(Member member) {
        removeMemberData(member);

        return ResponseEntity.ok(Map.of("result", member.getId()));
    }

    @Transactional
    public ResponseEntity<?> deleteMember(String email) {
        Member member = memberService.findMember(email);
        removeMemberData(member);
        return ResponseEntity.ok(Map.of("result", member.getId()));
    }

    private void removeMemberData(Member member) {
        // JWT authentication supplies a Redis-backed detached principal. Load
        // the managed aggregate only for this account-deletion operation,
        // which needs the member's lazy associations.
        member = memberService.findById(member.getId());

        accountService.deleteByMember(member);

        removeAuthoredBoardDependencies(member);

        appealService.detachSanctions(member);
        appealService.deleteByMember(member);
        memberSanctionService.deleteByMemberReferences(member);
        reportService.deleteByReporter(member);

        bookReviewService.deleteByAuthor(member);

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

        imageS3Service.deleteImgSrc(collectionService.findByMember(member).stream().map(Collection::getImgSrc).toList());

        collectionService.deleteByMember(member);

        removeDiaries(member);
        todoService.deleteByMember(member);
        memoService.deleteByMember(member);

        imageS3Service.deleteImage(member.getProfile());
        memberService.deleteById(member.getId());
    }

    private void removeAuthoredBoardDependencies(Member member) {
        for (Board board : member.getBoard()) {
            imageS3Service.deleteImage(board.getImg());
            boardImgService.deleteBoard(board);
            favoriteService.deleteByBoard(board);
            replyService.deleteReplay(board.getComment());
            commentService.deleteComment(board);
        }
    }

    private void removeDiaries(Member member) {
        for (Diary diary : member.getDiaries()) {
            imageS3Service.deleteImgSrc(diary.getImages().stream().map(DiaryImage::getImageUrl).toList());
            diaryImageService.deleteByDiary(diary);
        }
        diaryService.deleteByMember(member);
    }

    private ResponseEntity<?> invalidInput(String message) {
        return failure(HttpStatus.BAD_REQUEST, -10L, message);
    }

    private ResponseEntity<?> failure(HttpStatus status, long result, String message) {
        return ResponseEntity.status(status).body(Map.of("result", result, "message", message));
    }

    private ResponseEntity<?> memberResponse(Member member) {
        return ResponseEntity.ok(Map.of("result", member.getId(), "member", member));
    }

    public ResponseEntity<?> logout(Member member, HttpServletRequest request, HttpServletResponse response) {
        clearSession(request, response);

        return ResponseEntity.ok(Map.of(
                "result", member == null ? 0L : member.getId()
        ));
    }

    public ResponseEntity<?> recoverPassword(MemberRequestDto requestDto, Member loginMember) {
        if (loginMember != null) {
            return invalidInput("회원 설정의 비밀번호 변경 절차를 이용해주세요");
        }

        String password = requestDto.getPassword();
        if (!StringUtils.hasText(password)) return invalidInput("비밀번호가 입력되지 않았습니다");
        String failureMessage = "5분 이내에 가입한 이메일의 인증을 완료한 후 다시 시도해 주세요";
        if (!emailVerificationService.consume(EmailVerificationPurpose.EMAIL, requestDto.getEmail(), requestDto.getVerificationToken()))
            return failure(HttpStatus.FORBIDDEN, -403L, failureMessage);

        Member member = memberService.findMemberOrNull(requestDto.getEmail());
        if (member == null) return failure(HttpStatus.FORBIDDEN, -403L, failureMessage);
        return updatePassword(password, member);
    }

    public ResponseEntity<?> sendPasswordVerification(Member member) {
        emailVerificationService.send(EmailVerificationPurpose.PASSWORD_CHANGE, member.getId().toString(), member.getEmail());

        return ResponseEntity.ok(Map.of(
                "result", member.getId(),
                "message", "인증번호를 이메일로 발송했습니다"
        ));
    }

    public ResponseEntity<?> verifyPasswordChange(String code, Member member) {
        emailVerificationService.verifyPasswordChange(member.getId().toString(), code);

        return ResponseEntity.ok(Map.of(
                "result", member.getId(),
                "message", "이메일 인증이 완료되었습니다"
        ));
    }

    public ResponseEntity<?> updateAuthenticatedPassword(String password, Member member, HttpServletRequest request, HttpServletResponse response) {
        if (!StringUtils.hasText(password)) return invalidInput("비밀번호가 입력되지 않았습니다");
        if (!emailVerificationService.consumePasswordChange(member.getId().toString())) {
            return failure(HttpStatus.FORBIDDEN, -10L, "이메일 인증 후 비밀번호를 변경할 수 있습니다");
        }

        ResponseEntity<?> result = updatePassword(password, member);

        clearSession(request, response);

        return result;
    }

    public ResponseEntity<?> withdraw(Member member, HttpServletRequest request, HttpServletResponse response) {
        ResponseEntity<?> result = transactions.write(() -> deleteMember(member));

        clearSession(request, response);

        return result;
    }

    private void establishSession(Member member, HttpServletRequest request, HttpServletResponse response, String ipAddress) {
        IssuedToken accessToken = tokenProvider.createAccessToken(member);
        IssuedToken refreshToken = tokenProvider.createRefreshToken(member);

        refreshTokenService.save(refreshToken.jti(), member, tokenProvider.refreshTokenDuration(), ipAddress);
        memberTokenCacheService.save(member, tokenProvider.refreshTokenDuration());

        if (request != null) {
            revokeSessionRefreshToken(cookieService.readCurrentRefreshToken(request));
            cookieService.rememberRefreshToken(request, refreshToken.value());
        }

        cookieService.writeAccessToken(response, accessToken.value(), tokenProvider.accessTokenDuration());
        cookieService.writeRefreshToken(response, refreshToken.value(), tokenProvider.refreshTokenDuration());
    }

    private void clearSession(HttpServletRequest request, HttpServletResponse response) {
        revokeSessionRefreshToken(cookieService.readCurrentRefreshToken(request));

        cookieService.clearAuthenticationCookies(response);

        SecurityContextHolder.clearContext();
    }

    private void revokeSessionRefreshToken(String tokenValue) {
        if (tokenValue == null || tokenValue.isBlank())
            return;

        try {
            TokenClaims claims = tokenProvider.parse(tokenValue, TokenName.REFRESH.getValue());
            RefreshToken oldToken = refreshTokenService.findOldToken(claims.jti());

            if (oldToken != null) {
                refreshTokenService.deleteOldToken(oldToken);

                if (!refreshTokenService.existsByMemberIdAndExpiresAtAfter(oldToken.getMemberId(), Instant.now())) {
                    memberTokenCacheService.delete(oldToken.getMemberId());
                }
            }
        } catch (JwtValidationException ignored) {
            // Logout remains idempotent for expired or malformed client tokens.
        }
    }
}
