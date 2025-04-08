package spring.study.repository.favorite;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import spring.study.entity.board.Board;
import spring.study.entity.favorite.Favorite;
import spring.study.entity.member.Member;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    Favorite findByMemberAndBoard(Member member, Board board);

    void deleteByMember(Member member);
    void deleteByBoard(Board board);

    Boolean existsByMemberAndBoard(Member member, Board board);
}
