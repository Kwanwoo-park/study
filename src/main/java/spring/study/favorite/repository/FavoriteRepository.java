package spring.study.favorite.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spring.study.board.entity.Board;
import spring.study.favorite.entity.Favorite;
import spring.study.member.entity.Member;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    Favorite findByMemberAndBoard(Member member, Board board);

    void deleteByMember(Member member);
    void deleteByBoard(Board board);

    Boolean existsByMemberAndBoard(Member member, Board board);
}
