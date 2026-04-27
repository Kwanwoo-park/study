package spring.study.favorite.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spring.study.board.entity.Board;
import spring.study.favorite.entity.Favorite;
import spring.study.member.entity.Member;

import java.util.List;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    Favorite findByMemberAndBoard(Member member, Board board);
    List<Favorite> findByBoard(Board board, Pageable pageable);

    List<Favorite> findByBoard(Board board);
    List<Favorite> findByMember(Member member);

    long countByBoard(Board board);

    void deleteByMember(Member member);
    void deleteByBoard(Board board);

    Boolean existsByMemberAndBoard(Member member, Board board);
}
