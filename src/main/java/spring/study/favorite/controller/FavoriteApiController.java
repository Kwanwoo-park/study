package spring.study.favorite.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.board.entity.Board;
import spring.study.favorite.entity.Favorite;
import spring.study.member.entity.Member;
import spring.study.notification.entity.Notification;
import spring.study.board.service.BoardService;
import spring.study.favorite.service.FavoriteService;
import spring.study.notification.service.NotificationService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/favorite")
public class FavoriteApiController {
    private final FavoriteService favoriteService;
    private final BoardService boardService;
    private final NotificationService notificationService;

    @PostMapping("/like")
    public ResponseEntity<Favorite> favoriteAction(@RequestParam Long id, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid() || session.getAttribute("member") == null)
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);

        Member member = (Member) session.getAttribute("member");
        Board board = boardService.findById(id);

        Member otherMember = board.getMember();

        if (favoriteService.existFavorite(member, board))
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(null);

        Favorite favorite = favoriteService.save(member, board);

        if (!member.getId().equals(otherMember.getId()))
            notificationService.createNotification(otherMember, member.getName() + "님이 게시글에 좋아요를 눌렀습니다").addMember(otherMember);

        session.setAttribute("member", member);

        return ResponseEntity.ok(favorite);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Favorite> favoriteDelete(@RequestParam Long id, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid() || session.getAttribute("member") == null)
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);

        Member member = (Member) session.getAttribute("member");
        Board board = boardService.findById(id);

        Favorite favorite = favoriteService.findByMemberAndBoard(member, board);

        if (favorite == null)
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(null);

        favoriteService.deleteById(favorite, member, board);

        session.setAttribute("member", member);

        return ResponseEntity.ok(favorite);
    }
}
