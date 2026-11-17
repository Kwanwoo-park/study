package spring.study.todo.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import spring.study.diary.entity.Diary;
import spring.study.diary.repository.DiaryRepository;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.repository.MemberRepository;
import spring.study.todo.entity.Todo;
import spring.study.todo.service.TodoService;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY, connection = EmbeddedDatabaseConnection.H2)
@Import(TodoService.class)
class TodoPersistenceTest {
    @Autowired private TodoRepository repository;
    @Autowired private TodoService service;
    @Autowired private MemberRepository members;
    @Autowired private DiaryRepository diaries;
    @Autowired private EntityManager entityManager;

    @Test
    void todosSurviveDiaryDeletionAndContentAndCompletionArePersisted() {
        Member member = members.save(member("owner@example.test", "01011112222"));
        Diary diary = diaries.save(Diary.builder().member(member).title("일기").content("본문").build());
        Long id = service.create("일기와 별개의 할 일", member).id();
        entityManager.flush();
        entityManager.clear();

        service.update(id, "수정한 내용", member);
        LocalDateTime completedAt = service.updateCompletion(id, true, member).completedAt();
        assertThat(completedAt).isNotNull();
        service.update(id, "수정한 내용", member);
        assertThat(service.updateCompletion(id, true, member).completedAt()).isEqualTo(completedAt);
        assertThat(service.updateCompletion(id, false, member).completedAt()).isNull();
        assertThat(service.updateCompletion(id, true, member).completedAt()).isAfterOrEqualTo(completedAt);
        diaries.deleteById(diary.getId());
        entityManager.flush();
        entityManager.clear();

        Todo saved = repository.findById(id).orElseThrow();
        assertThat(saved.getMember().getId()).isEqualTo(member.getId());
        assertThat(saved.getContent()).isEqualTo("수정한 내용");
        assertThat(saved.isCompleted()).isTrue();
        assertThat(saved.getCompletedAt()).isNotNull();
        assertThat(diaries.findById(diary.getId())).isEmpty();
        assertThat(entityManager.getMetamodel().getEntities()).noneMatch(type -> type.getName().equals("diary_todo"));
    }

    @Test
    void eachSectionSortsAndPagesByItsOwnTimestampWithoutIncludingAnotherMember() {
        Member owner = members.saveAndFlush(member("sort-owner@example.test", "01022223333"));
        Member other = members.saveAndFlush(member("sort-other@example.test", "01033334444"));
        LocalDateTime time = LocalDateTime.of(2026, 11, 17, 12, 0);
        Todo pendingNew = create(owner, "최근 생성", time, null);
        Todo pendingOld = create(owner, "이전 생성", time.minusDays(1), null);
        Todo completedNew = create(owner, "최근 완료", time.minusDays(3), time);
        Todo completedOld = create(owner, "이전 완료", time.minusDays(2), time.minusHours(1));
        create(other, "다른 회원", time.plusDays(1), time.plusDays(1));
        service.update(completedOld.getId(), "완료 후 내용 수정", owner);
        entityManager.flush();
        entityManager.clear();

        assertThat(service.findByMember(owner, false, 0, 20).getContent())
                .extracting(todo -> todo.id()).containsExactly(pendingNew.getId(), pendingOld.getId());
        assertThat(service.findByMember(owner, true, 0, 1).getContent())
                .extracting(todo -> todo.id()).containsExactly(completedNew.getId());
        assertThat(service.findByMember(owner, true, 1, 1).getContent())
                .extracting(todo -> todo.id()).containsExactly(completedOld.getId());
        assertThat(service.findById(completedOld.getId(), owner).completedAt()).isEqualTo(time.minusHours(1));
    }

    private Todo create(Member member, String content, LocalDateTime createdAt, LocalDateTime completedAt) {
        Todo todo = repository.saveAndFlush(Todo.builder().member(member).content(content).completed(completedAt != null).build());
        entityManager.createQuery("update todo t set t.registerTime = :createdAt, t.completedAt = :completedAt where t.id = :id")
                .setParameter("createdAt", createdAt).setParameter("completedAt", completedAt).setParameter("id", todo.getId()).executeUpdate();
        entityManager.clear();
        return todo;
    }

    private Member member(String email, String phone) {
        return Member.builder().email(email).pwd("test-password").name("회원").role(Role.USER)
                .phone(phone).birth("20000101").profile("profile.png").build();
    }
}
