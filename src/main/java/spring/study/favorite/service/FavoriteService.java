package spring.study.favorite.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import spring.study.board.entity.Board;
import spring.study.favorite.entity.Favorite;
import spring.study.member.entity.Member;
import spring.study.favorite.repository.FavoriteRepository;

@RequiredArgsConstructor
@Service
public class FavoriteService {
    private final FavoriteRepository favoriteRepository;

    @Transactional
    public Favorite save(Member member, Board board) {
        Favorite favorite = Favorite.builder()
                .board(board)
                .member(member)
                .build();

        member.addFavorite(favorite);
        board.addFavorite(favorite);

        return favoriteRepository.save(favorite);
    }

    public Favorite findByMemberAndBoard(Member member, Board board) {
        return favoriteRepository.findByMemberAndBoard(member, board);
    }

    public Boolean existFavorite(Member member, Board board) {
        return favoriteRepository.existsByMemberAndBoard(member, board);
    }

    @Transactional
    public void deleteById(Favorite favorite, Member member, Board board) {
        member.removeFavorite(favorite);
        board.removeFavorite(favorite);

        favoriteRepository.deleteById(favorite.getId());
    }

    @Transactional
    public void deleteByMember(Member member) {
        favoriteRepository.deleteByMember(member);
    }

    @Transactional
    public void deleteByBoard(Board board) {
        favoriteRepository.deleteByBoard(board);
    }
}
