package spring.study.board.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import spring.study.board.dto.BoardBookmarkDto;
import spring.study.board.entity.Board;
import spring.study.board.service.BoardBookmarkService;
import spring.study.common.entity.CommonVisibility;
import spring.study.follow.entity.Follow;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop", showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY, connection = EmbeddedDatabaseConnection.H2)
@Import(BoardBookmarkService.class)
class BoardBookmarkPersistenceTest {
    @Autowired private TestEntityManager em;
    @Autowired private BoardBookmarkService bookmarks;

    @Test
    void privateBookmarksFollowCurrentPostPermissionsAndAreRemovedWithThePost() {
        Member author = member("author@example.test", "01011112222");
        Member reader = member("reader@example.test", "01011113333");
        Member other = member("other@example.test", "01011114444");
        Board board = em.persist(Board.builder().content("저장할 글").member(author).build());
        bookmarks.save(reader, board);
        bookmarks.save(reader, board);
        assertThat(bookmarks.list(reader, 0).getContent()).extracting(BoardBookmarkDto::boardId).containsExactly(board.getId());
        assertThat(bookmarks.list(other, 0).getContent()).isEmpty();

        board.changeVisibility(CommonVisibility.PRIVATE);
        assertThat(bookmarks.list(reader, 0).getTotalElements()).isZero();
        em.persist(Follow.builder().follower(reader).following(author).build());
        assertThat(bookmarks.list(reader, 0).getContent()).hasSize(1);

        em.remove(board);
        em.flush();
        em.clear();
        assertThat(bookmarks.contains(reader, board.getId())).isFalse();
    }

    private Member member(String email, String phone) {
        return em.persist(Member.builder().email(email).name("회원").pwd("test").phone(phone).birth("2000-01-01")
                .profile("profile.png").role(Role.USER).build());
    }
}
