package spring.study.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import spring.study.entity.Board;

import java.util.List;

public interface TestRepository extends JpaRepository<Board, Long> {
    static final String join = "select * from board b, member m where b.register_email = m.email and b.register_name = :name";

    @Transactional
    @Query(value = join, nativeQuery = true)
    List<Object[]> getBoardWithMember(@Param("name") String name);
}
