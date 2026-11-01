package spring.study.favorite.facade;

import org.junit.jupiter.api.Test;
import spring.study.board.entity.Board;
import spring.study.board.service.BoardService;
import spring.study.common.service.VisibilityAccessPolicy;
import spring.study.favorite.entity.Favorite;
import spring.study.favorite.service.FavoriteService;
import spring.study.follow.entity.Follow;
import spring.study.follow.service.FollowService;
import spring.study.member.entity.Member;
import spring.study.notification.service.NotificationService;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class FavoriteFacadeTest {
    private final FavoriteService favorites = mock(FavoriteService.class);
    private final FollowService follows = mock(FollowService.class);
    private final BoardService boards = mock(BoardService.class);
    private final VisibilityAccessPolicy visibility = mock(VisibilityAccessPolicy.class);
    private final FavoriteFacade facade = new FavoriteFacade(favorites, follows, boards,
            mock(NotificationService.class), visibility);
    private final Member viewer = member(1L);
    private final Member followed = spy(member(3L));
    private final Member notFollowed = spy(member(4L));
    private final Board board = Board.builder().id(20L).member(viewer).content("본문").build();

    @Test
    void checksLikedMembersByIdWithoutReadingTheirFollowCollections() {
        prepareFavorites();
        when(follows.findByFollower(viewer)).thenReturn(List.of(
                Follow.builder().id(31L).follower(viewer).following(followed).build()));

        Map<?, ?> body = (Map<?, ?>) facade.getList(20L, viewer, 0, 2).getBody();

        assertThat(body.get("following")).isEqualTo(Map.of(11L, true, 12L, false));
        assertThat(body.get("nextCursor")).isEqualTo(2);
        assertThat(body.get("totalCount")).isEqualTo(3L);
        verify(followed, never()).getFollowing();
        verify(notFollowed, never()).getFollowing();
    }

    @Test
    void marksEveryLikedMemberFalseWhenTheViewerFollowsNobody() {
        prepareFavorites();
        when(follows.findByFollower(viewer)).thenReturn(List.of());

        Map<?, ?> body = (Map<?, ?>) facade.getList(20L, viewer, 0, 2).getBody();

        assertThat(body.get("following")).isEqualTo(Map.of(11L, false, 12L, false));
    }

    private void prepareFavorites() {
        when(boards.findById(20L)).thenReturn(board);
        when(visibility.canViewBoard(board, viewer)).thenReturn(true);
        when(favorites.countFavorites(board)).thenReturn(3L);
        when(favorites.getFavorites(board, 0, 2)).thenReturn(List.of(
                Favorite.builder().id(11L).member(followed).board(board).build(),
                Favorite.builder().id(12L).member(notFollowed).board(board).build()));
    }

    private Member member(Long id) {
        return Member.builder().id(id).email("member" + id + "@example.test").build();
    }
}
