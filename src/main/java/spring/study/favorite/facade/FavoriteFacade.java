package spring.study.favorite.facade;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import spring.study.board.entity.Board;
import spring.study.board.service.BoardService;
import spring.study.favorite.dto.FavoriteResponseDto;
import spring.study.favorite.entity.Favorite;
import spring.study.favorite.service.FavoriteService;
import spring.study.follow.entity.Follow;
import spring.study.follow.service.FollowService;
import spring.study.member.entity.Member;
import spring.study.notification.entity.Group;
import spring.study.notification.service.NotificationService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class FavoriteFacade {
    private final FavoriteService favoriteService;
    private final FollowService followService;
    private final BoardService boardService;
    private final NotificationService notificationService;

    public ResponseEntity<?> getList(Long id, Member member, int cursor, int limit) {
        Board board = boardService.findById(id);
        long totalCount = favoriteService.countFavorites(board);
        List<Favorite> favorites = favoriteService.getFavorites(board, cursor, limit);
        int nextCursor = (long) (cursor + 1) * limit >= totalCount ? 0 : cursor + 2;

        return ResponseEntity.ok(Map.of(
                "list", favorites.stream().map(FavoriteResponseDto::new).toList(),
                "email", member.getEmail(),
                "following", checkFollowing(favorites, followService.findByFollower(member)),
                "totalCount", totalCount,
                "nextCursor", nextCursor,
                "result", 10L
        ));
    }

    public ResponseEntity<?> like(Long id, Member member, HttpServletRequest request) {
        Board board = boardService.findById(id);

        if (board == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -1L,
                    "message", "존재하지 않는 게시글입니다"
            ));
        }

        if (favoriteService.existFavorite(member, board)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -1L,
                    "message", "이미 좋아요를 눌렀습니다"
            ));
        }

        Favorite favorite = favoriteService.save(member, board);
        Member otherMember = board.getMember();

        if (!member.getId().equals(otherMember.getId()))
            notificationService.createNotification(otherMember,
                    member.getName() + "님이 게시글에 좋아요를 눌렀습니다",
                    Group.FAVORITE,
                    id.toString()
            ).addMember(otherMember);

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

        return ResponseEntity.ok(Map.of(
                "result", favorite.getId()
        ));
    }

    private HashMap<Long, Boolean> checkFollowing(List<Favorite> boardFavorites, List<Follow> memberFollower) {
        HashMap<Long, Boolean> map = new HashMap<>();

        for (Favorite f : boardFavorites) {
            map.put(f.getId(), false);

            for (Follow following : f.getMember().getFollowing()) {
                for (Follow follower : memberFollower) {
                    if (Objects.equals(follower.getId(), following.getId())) {
                        map.put(f.getId(), true);
                        break;
                    }
                }
            }
        }

        return map;
    }
}
