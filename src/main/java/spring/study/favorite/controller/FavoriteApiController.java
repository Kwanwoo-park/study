package spring.study.favorite.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.board.entity.Board;
import spring.study.favorite.entity.Favorite;
import spring.study.member.entity.Member;
import spring.study.notification.entity.Group;
import spring.study.notification.entity.Notification;
import spring.study.board.service.BoardService;
import spring.study.favorite.service.FavoriteService;
import spring.study.notification.service.NotificationService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/favorite")
public class FavoriteApiController {
    private final FavoriteService favoriteService;
    private final BoardService boardService;
    private final NotificationService notificationService;

    @PostMapping("/like")
    public ResponseEntity<Map<String, Long>> favoriteAction(@RequestParam Long id, HttpServletRequest request) {
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

        try {
            Board board = boardService.findById(id);

            Member otherMember = board.getMember();

            if (favoriteService.existFavorite(member, board)) {
                map.put("result", -1L);
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(map);
            }

            Favorite favorite = favoriteService.save(member, board);
            map.put("result", favorite.getId());

            if (!member.getId().equals(otherMember.getId()))
                notificationService.createNotification(otherMember, member.getName() + "님이 게시글에 좋아요를 눌렀습니다", Group.FAVORITE).addMember(otherMember);

            session.setAttribute("member", member);

            return ResponseEntity.ok(map);
        } catch (Exception e) {
            log.error(e.getMessage());

            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Map<String, Long>> favoriteDelete(@RequestParam Long id, HttpServletRequest request) {
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

        try {
            Board board = boardService.findById(id);

            Favorite favorite = favoriteService.findByMemberAndBoard(member, board);

            if (favorite == null) {
                map.put("result", -10L);
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(map);
            }

            map.put("result", favorite.getId());

            favoriteService.deleteById(favorite, member, board);

            session.setAttribute("member", member);

            return ResponseEntity.ok(map);
        } catch(Exception e) {
            log.error(e.getMessage());
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
        }
    }
}
