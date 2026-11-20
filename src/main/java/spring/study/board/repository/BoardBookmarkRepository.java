package spring.study.board.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import spring.study.board.dto.BoardBookmarkDto;
import spring.study.board.entity.BoardBookmark;
import spring.study.common.entity.CommonVisibility;
import spring.study.member.entity.Member;

import java.util.List;

public interface BoardBookmarkRepository extends JpaRepository<BoardBookmark, Long> {
    @Query("""
            select new spring.study.board.dto.BoardBookmarkDto(b.id, b.member.name, b.content)
            from BoardBookmark bm join bm.board b
            where bm.member = :member and (b.visibility = :visibility or b.member = :member
                or exists (select f.id from follow f where f.follower = :member and f.following = b.member))
            order by bm.id desc
            """)
    Page<BoardBookmarkDto> findVisible(@Param("member") Member member, @Param("visibility") CommonVisibility visibility,
                                       Pageable pageable);

    @Query("select bm.board.id from BoardBookmark bm where bm.member = :member and bm.board.id in :ids")
    List<Long> findSavedIds(@Param("member") Member member, @Param("ids") List<Long> ids);

    boolean existsByMemberAndBoardId(Member member, Long boardId);

    void deleteByMemberAndBoardId(Member member, Long boardId);
}
