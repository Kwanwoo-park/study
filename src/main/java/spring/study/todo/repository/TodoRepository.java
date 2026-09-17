package spring.study.todo.repository;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import spring.study.member.entity.Member;
import spring.study.todo.entity.Todo;

import java.util.Optional;

public interface TodoRepository extends JpaRepository<Todo, Long> {
    @Transactional(readOnly = true)
    Page<Todo> findByMember(Member member, Pageable pageable);

    @Transactional(readOnly = true)
    Page<Todo> findByMemberAndCompleted(Member member, boolean completed, Pageable pageable);

    @Transactional(readOnly = true)
    Optional<Todo> findByIdAndMember(Long id, Member member);

    void deleteByMember(Member member);
}
