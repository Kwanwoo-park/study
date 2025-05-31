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
    public Favorite save(Favorite favorite) {
        return favoriteRepository.save(favorite);
    }

    public Favorite findByMemberAndBoard(Member member, Board board) {
        return favoriteRepository.findByMemberAndBoard(member, board);
    }

    public Boolean existFavorite(Member member, Board board) {
        return favoriteRepository.existsByMemberAndBoard(member, board);
    }

    @Transactional
    public void deleteById(Long id) {
        favoriteRepository.deleteById(id);
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
