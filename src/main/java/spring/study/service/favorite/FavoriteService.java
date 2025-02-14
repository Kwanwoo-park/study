package spring.study.service.favorite;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import spring.study.entity.board.Board;
import spring.study.entity.favorite.Favorite;
import spring.study.entity.member.Member;
import spring.study.repository.favorite.FavoriteRepository;

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

    @Transactional
    public void deleteById(Long id) {
        favoriteRepository.deleteById(id);
    }
}
