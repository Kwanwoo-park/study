package spring.study.controller.favorite;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.entity.board.Board;
import spring.study.entity.favorite.Favorite;
import spring.study.entity.member.Member;
import spring.study.service.board.BoardService;
import spring.study.service.favorite.FavoriteService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/favorite")
public class FavoriteApiController {
    private final FavoriteService favoriteService;
    private final BoardService boardService;

    @PostMapping("/like")
    public ResponseEntity<Favorite> favoriteAction(@RequestParam Long id, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid() || session.getAttribute("member") == null)
            return ResponseEntity.status(501).body(null);

        Member member = (Member) session.getAttribute("member");
        Board board = boardService.findById(id);

        Favorite favorite = Favorite.builder()
                .board(board)
                .member(member)
                .build();

        member.addFavorite(favorite);
        board.addFavorite(favorite);

        session.setAttribute("member", member);

        return ResponseEntity.ok(favoriteService.save(favorite));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Favorite> favoriteDelete(@RequestParam Long id, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid() || session.getAttribute("member") == null)
            return ResponseEntity.status(501).body(null);

        Member member = (Member) session.getAttribute("member");
        Board board = boardService.findById(id);

        Favorite favorite = favoriteService.findByMemberAndBoard(member, board);

        member.removeFavorite(favorite);
        board.removeFavorite(favorite);

        favoriteService.deleteById(favorite.getId());

        session.setAttribute("member", member);

        return ResponseEntity.ok(favorite);
    }
}
