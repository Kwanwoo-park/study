package spring.study.board.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.board.dto.BoardRequestDto;
import spring.study.board.entity.Board;
import spring.study.board.entity.BoardImg;
import spring.study.comment.entity.Comment;
import spring.study.forbidden.entity.Status;
import spring.study.member.dto.MemberResponseDto;
import spring.study.member.entity.Member;
import spring.study.aws.service.ImageS3Service;
import spring.study.board.service.BoardImgService;
import spring.study.board.service.BoardService;
import spring.study.comment.service.CommentService;
import spring.study.favorite.service.FavoriteService;
import spring.study.forbidden.service.ForbiddenService;
import spring.study.member.entity.Role;
import spring.study.member.service.MemberService;
import spring.study.notification.service.NotificationService;
import spring.study.reply.service.ReplyService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/board")
public class BoardApiController {
    private final BoardService boardService;
    private final ReplyService replyService;
    private final CommentService commentService;
    private final BoardImgService boardImgService;
    private final FavoriteService favoriteService;
    private final ImageS3Service imageS3Service;
    private final ForbiddenService forbiddenService;
    private final MemberService memberService;
    private final NotificationService notificationService;

    @PostMapping("/write")
    public ResponseEntity<Long> boardWriteAction(@RequestBody BoardRequestDto boardRequestDto, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);

        if (session.getAttribute("member") == null) {
            session.invalidate();
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);
        }

        Member member = (Member) session.getAttribute("member");

        if (!boardRequestDto.getContent().isBlank() || !boardRequestDto.getContent().isEmpty()){
            int risk = forbiddenService.findWordList(Status.APPROVAL, boardRequestDto.getContent());

            if (risk != 0) {
                if (risk == 3) {
                    notificationService.createNotification(memberService.findAdministrator(), member.getName() + "님이 금칙어를 사용하여 차단하였습니다");
                    memberService.updateRole(member.getId(), Role.DENIED);

                    session.invalidate();

                    return ResponseEntity.ok(-3L);
                }

                return ResponseEntity.ok(-1L);
            }

            Board board = boardRequestDto.toEntity();

            board.addMember(member);

            Board result = boardService.save(board);
            session.setAttribute("member", member);

            if (result == null) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(null);
            }

            return ResponseEntity.ok(result.getId());
        }
        else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @PatchMapping("/view")
    public ResponseEntity<Integer> boardViewAction(@RequestBody BoardRequestDto boardRequestDto, HttpServletRequest request){
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);

        if (session.getAttribute("member") == null) {
            session.invalidate();
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);
        }

        Member member = (Member) session.getAttribute("member");

        if (!boardRequestDto.getContent().isBlank() | !boardRequestDto.getContent().isEmpty()) {
            int risk = forbiddenService.findWordList(Status.APPROVAL, boardRequestDto.getContent());

            if (risk != 0) {
                if (risk == 3) {
                    notificationService.createNotification(memberService.findAdministrator(), member.getName() + "님이 금칙어를 사용하여 차단하였습니다");
                    memberService.updateRole(member.getId(), Role.DENIED);

                    session.invalidate();

                    return ResponseEntity.ok(-3);
                }

                return ResponseEntity.ok(-1);
            }

            return ResponseEntity.ok(boardService.updateBoard(boardRequestDto.getId(), boardRequestDto.getContent()));
        }
        else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @DeleteMapping("/view/delete")
    public ResponseEntity<Map<String, String>> boardViewDeleteAction(@RequestParam() Long id, HttpServletRequest request){
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);

        if (session.getAttribute("member") == null) {
            session.invalidate();
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);
        }

        Member member = (Member) session.getAttribute("member");
        Board board = boardService.findById(id);

        Map<String, String> emailMap = new HashMap<>();
        emailMap.put("email", member.getEmail());

        member.removeComments(board.getComment());
        member.removeBoard(board);

        session.setAttribute("member", member);

        favoriteService.deleteByBoard(board);

        for (Comment comment : board.getComment()) {
            replyService.deleteReply(comment);
        }

        commentService.deleteComment(board);

        List<BoardImg> images = boardService.findById(id).getImg();

        for (BoardImg img : images) {
            imageS3Service.deleteImage(img.getImgSrc());
        }

        boardImgService.deleteBoard(board);
        boardService.deleteById(id);

        return ResponseEntity.status(HttpStatus.OK).body(emailMap);
    }
}
