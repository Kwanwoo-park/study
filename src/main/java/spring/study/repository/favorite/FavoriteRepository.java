package spring.study.repository.favorite;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import spring.study.entity.board.Board;
import spring.study.entity.favorite.Favorite;
import spring.study.entity.member.Member;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    public Favorite findByMemberAndBoard(Member member, Board board);

    public void deleteByMember(Member member);
    public void deleteByBoard(Board board);

    public Boolean existsByMemberAndBoard(Member member, Board board);
}
