package spring.study.favorite.facade;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import spring.study.board.entity.Board;
import spring.study.board.service.BoardService;
import spring.study.favorite.entity.Favorite;
import spring.study.favorite.service.FavoriteService;
import spring.study.member.entity.Member;
import spring.study.member.service.MemberService;
import spring.study.notification.entity.Group;
import spring.study.notification.service.NotificationService;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FavoriteFacade {
    private final FavoriteService favoriteService;
    private final BoardService boardService;
    private final MemberService memberService;
    private final NotificationService notificationService;

    public ResponseEntity<?> like(Long id, Member member, HttpServletRequest request) {
        Board board = boardService.findById(id);

        if (favoriteService.existFavorite(member, board)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -1L,
                    "message", "이미 좋아요를 눌렀습니다"
            ));
        }

        Favorite favorite = favoriteService.save(member, board);
        Member otherMember = board.getMember();

        if (!member.getId().equals(otherMember.getId()))
            notificationService.createNotification(otherMember, member.getName() + "님이 게시글에 좋아요를 눌렀습니다", Group.FAVORITE).addMember(otherMember);

        request.getSession(false).setAttribute("member", member);

        return ResponseEntity.ok(Map.of(
                "result", favorite.getId()
        ));
    }

    public ResponseEntity<?> unlike(Long id, Member member, HttpServletRequest request) {
        Board board = boardService.findById(id);

        Favorite favorite = favoriteService.findByMemberAndBoard(member, board);

        if (favorite == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -10L,
                    "message", "이미 취소된 좋아요입니다"
            ));
        }

        favoriteService.deleteById(favorite, member, board);

        request.getSession(false).setAttribute("member", member);

        return ResponseEntity.ok(Map.of(
                "result", favorite.getId()
        ));
    }
}
