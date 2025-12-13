package spring.study.board.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.board.dto.BoardRequestDto;
import spring.study.board.dto.BoardResponseDto;
import spring.study.board.entity.Board;
import spring.study.forbidden.entity.Status;
import spring.study.member.entity.Member;
import spring.study.aws.service.ImageS3Service;
import spring.study.board.service.BoardImgService;
import spring.study.board.service.BoardService;
import spring.study.comment.service.CommentService;
import spring.study.favorite.service.FavoriteService;
import spring.study.forbidden.service.ForbiddenService;
import spring.study.member.entity.Role;
import spring.study.member.service.MemberService;
import spring.study.notification.entity.Group;
import spring.study.notification.service.NotificationService;
import spring.study.reply.service.ReplyService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/board")
@Slf4j
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

    @GetMapping("/load")
    public ResponseEntity<Map<String, Object>> getBoards(@RequestParam(defaultValue = "0", name = "cursor") int cursor,
                                                         @RequestParam(defaultValue = "10", name = "limit") int limit,
                                                         HttpServletRequest request) {
        HttpSession session = request.getSession();
        Map<String, Object> response = new HashMap<>();

        if (session == null || !request.isRequestedSessionIdValid()) {
            response.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(response);
        }

        if (session.getAttribute("member") == null) {
            session.invalidate();
            response.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(response);
        }

        Member member = (Member) session.getAttribute("member");

        List<BoardResponseDto> list;
        int nextCursor;

        try {
            list = boardService.getBoard(cursor, limit, member);
        } catch (Exception e) {
            log.error(e.getMessage());
            response.put("result", -10L);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        if (list.isEmpty())
            nextCursor = 0;
        else
            nextCursor = cursor+2;

        response.put("boards", list);
        response.put("nextCursor", nextCursor);
        response.put("like", member.checkFavorite(list));
        response.put("result", (long) list.size());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/write")
    public ResponseEntity<Map<String, Long>> boardWriteAction(@RequestBody BoardRequestDto boardRequestDto, HttpServletRequest request) {
        HttpSession session = request.getSession();
        Map<String, Long> map = new HashMap<>();

        if (session == null || !request.isRequestedSessionIdValid()) {
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        if (session.getAttribute("member") == null) {
            map.put("result", -10L);
            session.invalidate();
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        Member member = (Member) session.getAttribute("member");

        if (!boardRequestDto.getContent().isBlank() || !boardRequestDto.getContent().isEmpty()){
            try {
                int risk = forbiddenService.findWordList(Status.APPROVAL, boardRequestDto.getContent());

                if (risk != 0) {
                    if (risk == 3) {
                        notificationService.createNotification(memberService.findAdministrator(), member.getName() + "님이 금칙어를 사용하여 차단하였습니다", Group.ADMIN);
                        memberService.updateRole(member.getId(), Role.DENIED);

                        session.invalidate();
                        map.put("result", -3L);
                    } else
                        map.put("result", -1L);

                    return ResponseEntity.ok(map);
                }

                Board board = boardRequestDto.toEntity();
                board.addMember(member);

                Board result = boardService.save(board);
                session.setAttribute("member", member);

                if (result == null) {
                    map.put("result", -10L);
                    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(map);
                }

                map.put("result", result.getId());

                return ResponseEntity.ok(map);
            } catch (Exception e) {
                log.error(e.getMessage());
                map.put("result", -10L);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
            }
        }
        else {
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(map);
        }
    }

    @PatchMapping("/view")
    public ResponseEntity<Map<String, Long>> boardViewAction(@RequestBody BoardRequestDto boardRequestDto, HttpServletRequest request){
        HttpSession session = request.getSession();
        Map<String, Long> map = new HashMap<>();

        if (session == null || !request.isRequestedSessionIdValid()) {
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        if (session.getAttribute("member") == null) {
            session.invalidate();
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        Member member = (Member) session.getAttribute("member");

        if (!boardRequestDto.getContent().isBlank() || !boardRequestDto.getContent().isEmpty()) {
            try {
                int risk = forbiddenService.findWordList(Status.APPROVAL, boardRequestDto.getContent());

                if (risk != 0) {
                    if (risk == 3) {
                        notificationService.createNotification(memberService.findAdministrator(), member.getName() + "님이 금칙어를 사용하여 차단하였습니다", Group.ADMIN);
                        memberService.updateRole(member.getId(), Role.DENIED);

                        session.invalidate();
                        map.put("result", -3L);
                    } else
                        map.put("result", -1L);

                    return ResponseEntity.ok(map);
                }

                map.put("result", boardService.updateBoard(boardRequestDto.getId(), boardRequestDto.getContent()));

                return ResponseEntity.ok(map);
            } catch (Exception e) {
                log.error(e.getMessage());
                map.put("result", -10L);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
            }
        }
        else {
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(map);
        }
    }

    @DeleteMapping("/view/delete")
    public ResponseEntity<Map<String, Object>> boardViewDeleteAction(@RequestParam() Long id, HttpServletRequest request){
        HttpSession session = request.getSession();
        Map<String, Object> map = new HashMap<>();

        if (session == null || !request.isRequestedSessionIdValid()) {
            map.put("result", -1L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        if (session.getAttribute("member") == null) {
            session.invalidate();
            map.put("result", -1L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        Member member = (Member) session.getAttribute("member");

        try {
            Board board = boardService.findById(id);

            map.put("email", member.getEmail());

            member.removeComments(board.getComment());
            member.removeBoard(board);

            session.setAttribute("member", member);

            favoriteService.deleteByBoard(board);

            replyService.deleteReplay(board.getComment());

            commentService.deleteComment(board);

            imageS3Service.deleteImage(board.getImg());

            boardImgService.deleteBoard(board);
            boardService.deleteById(id);

            return ResponseEntity.status(HttpStatus.OK).body(map);
        } catch (Exception e) {
            log.error(e.getMessage());
            map.put("result", -1L);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
        }
    }
}
