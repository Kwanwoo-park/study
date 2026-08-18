package spring.study.favorite.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import spring.study.board.entity.Board;
import spring.study.favorite.entity.Favorite;
import spring.study.member.entity.Member;

import java.util.List;
import java.util.Collection;

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

    @Query("select f.board.id, count(f.id) from favorite f where f.board.id in :boardIds group by f.board.id")
    List<Object[]> countByBoardIds(@Param("boardIds") Collection<Long> boardIds);

    @Query("select f.board.id from favorite f where f.member = :member and f.board.id in :boardIds")
    List<Long> findLikedBoardIds(@Param("member") Member member, @Param("boardIds") Collection<Long> boardIds);
}
