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
        service.updateCompletion(id, true, member);
        diaries.deleteById(diary.getId());
        entityManager.flush();
        entityManager.clear();

        Todo saved = repository.findById(id).orElseThrow();
        assertThat(saved.getMember().getId()).isEqualTo(member.getId());
        assertThat(saved.getContent()).isEqualTo("수정한 내용");
        assertThat(saved.isCompleted()).isTrue();
        assertThat(diaries.findById(diary.getId())).isEmpty();
        assertThat(entityManager.getMetamodel().getEntities()).noneMatch(type -> type.getName().equals("diary_todo"));
    }

    @Test
    void memberScopedQueriesAndWithdrawalCleanupDoNotAffectOtherMembers() {
        Member first = members.save(member("first@example.test", "01011112222"));
        Member second = members.save(member("second@example.test", "01033334444"));
        Long active = service.create("미완료", first).id();
        Long completed = service.create("완료", first).id();
        service.updateCompletion(completed, true, first);
        Long other = service.create("다른 회원", second).id();
        entityManager.flush();
        entityManager.clear();

        assertThat(service.findByMember(first, null, 0, 20).getTotalElements()).isEqualTo(2);
        assertThat(service.findByMember(first, false, 0, 20).getContent()).extracting("id").containsExactly(active);
        assertThat(service.findByMember(first, true, 0, 20).getContent()).extracting("id").containsExactly(completed);
        assertThat(repository.findByIdAndMember(other, first)).isEmpty();

        service.deleteByMember(first);
        members.deleteById(first.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findAll()).extracting(Todo::getId).containsExactly(other);
        assertThat(members.findById(first.getId())).isEmpty();
        assertThat(members.findById(second.getId())).isPresent();
    }

    private Member member(String email, String phone) {
        return Member.builder().email(email).pwd("test-password").name("회원").role(Role.USER)
                .phone(phone).birth("20000101").profile("profile.png").build();
    }
}
