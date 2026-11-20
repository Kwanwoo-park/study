package spring.study.todo.repository;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import spring.study.member.entity.Member;
import spring.study.todo.entity.Todo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public interface TodoRepository extends JpaRepository<Todo, Long> {
    @Transactional(readOnly = true)
    Page<Todo> findByMember(Member member, Pageable pageable);

    @Transactional(readOnly = true)
    Page<Todo> findByMemberAndCompleted(Member member, boolean completed, Pageable pageable);

    Page<Todo> findByMemberAndCompletedFalseAndDueDateBefore(Member member, LocalDate today, Pageable pageable);

    Page<Todo> findByMemberAndCompletedFalseAndDueDate(Member member, LocalDate today, Pageable pageable);

    Page<Todo> findByMemberAndCompletedFalseAndDueDateAfter(Member member, LocalDate today, Pageable pageable);

    Page<Todo> findByMemberAndCompletedTrueAndCompletedAtGreaterThanEqualAndCompletedAtLessThan(
            Member member, LocalDateTime start, LocalDateTime end, Pageable pageable);

    @Transactional(readOnly = true)
    Optional<Todo> findByIdAndMember(Long id, Member member);

    void deleteByMember(Member member);
}
