package spring.study.memo.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import spring.study.memo.dto.MemoRequestDto;
import spring.study.memo.dto.MemoResponseDto;
import spring.study.memo.dto.MemoDraft;
import java.time.LocalDateTime;
import spring.study.memo.dto.MemoSummaryDto;
import spring.study.memo.entity.MemoColor;
import spring.study.memo.service.MemoService;
import spring.study.common.exception.BusinessStateException;
import spring.study.common.exception.ResourceNotFoundException;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.repository.MemberRepository;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY, connection = EmbeddedDatabaseConnection.H2)
@Import(MemoService.class)
class MemoPersistenceTest {
    @Autowired MemoService service;
    @Autowired MemoRepository memos;
    @Autowired MemberRepository members;

    @Test
    void searchesTitleAndBodyLiterallyAndKeepsEveryResultOwnerScoped() {
        Member owner = member("owner");
        Member other = member("other");
        service.create(request("Release checklist", "준비", false, null), owner);
        service.create(request("비용", "부하 100%_완료!", true, null), owner);
        service.create(request("private", "Release checklist 100%_완료!", true, null), other);

        assertThat(service.list(owner, "release", false, 0).content()).extracting(MemoSummaryDto::title).containsExactly("Release checklist");
        assertThat(service.list(owner, "%_", false, 0).content()).extracting(MemoSummaryDto::title).containsExactly("비용");
        assertThat(service.list(owner, "", true, 0).content()).extracting(MemoSummaryDto::title).containsExactly("비용");
        assertThat(service.list(owner, "", false, 0).content()).extracting(MemoSummaryDto::title).containsExactly("비용", "Release checklist");
        assertThat(service.list(owner, "", false, 0).totalCount()).isEqualTo(2);
    }

    @Test
    void otherMembersCannotReadPersistOrDeleteAMemoByGuessingItsId() {
        Member owner = member("owner");
        Member other = member("other");
        MemoResponseDto memo = service.create(request("비공개", "원본", false, null), owner);

        assertThatThrownBy(() -> service.find(memo.id(), other)).isInstanceOf(ResourceNotFoundException.class);
        MemoDraft foreign = new MemoDraft(memo, other.getId(), memo.version(), 1, 1, 0, 0, false, false, "operation");
        assertThat(service.persist(foreign)).isFalse();
        service.deleteStored(memo.id(), other.getId());
        assertThat(memos.findById(memo.id()).orElseThrow().getContent()).isEqualTo("원본");
    }

    @Test
    void queuedSnapshotsRespectDbVersionsAndRetriesAreIdempotent() {
        Member owner = member("owner");
        MemoResponseDto original = service.create(request("처음", "원본", false, null), owner);
        MemoResponseDto snapshot = new MemoResponseDto(original.id(), "지연 저장", "최신 Redis 본문", MemoColor.MINT, true,
                original.version() + 3, original.createdAt(), LocalDateTime.now());
        MemoDraft draft = new MemoDraft(snapshot, owner.getId(), original.version(), 1, 1, 0, 0, false, false, "operation");
        assertThat(service.persist(draft)).isTrue();
        assertThat(service.persist(draft)).isTrue();
        assertThat(service.find(original.id(), owner).version()).isEqualTo(snapshot.version());
        assertThat(service.find(original.id(), owner).content()).isEqualTo(snapshot.content());
        MemoDraft stale = new MemoDraft(original, owner.getId(), original.version(), 1, 1, 0, 0, false, false, "operation");
        assertThatThrownBy(() -> service.persist(stale)).isInstanceOf(BusinessStateException.class);
        MemoDraft foreign = new MemoDraft(snapshot, owner.getId() + 1, snapshot.version(), 1, 1, 0, 0, false, false, "operation");
        assertThat(service.persist(foreign)).isFalse();
    }

    @Test
    void settingsAreSavedWithoutChangingTitleOrContent() {
        Member owner = member("owner");
        MemoResponseDto original = service.create(request("제목", "저장된 본문", false, null), owner);

        MemoResponseDto updated = new MemoResponseDto(original.id(), original.title(), original.content(), MemoColor.MINT, true,
                original.version() + 1, original.createdAt(), LocalDateTime.now().withNano(0));
        assertThat(service.persist(new MemoDraft(updated, owner.getId(), original.version(), 1, 1, 0, 0, false, false, "operation"))).isTrue();

        assertThat(updated.version()).isGreaterThan(original.version());
        assertThat(updated.title()).isEqualTo(original.title());
        assertThat(updated.content()).isEqualTo(original.content());
        assertThat(updated.color()).isEqualTo(MemoColor.MINT);
        assertThat(updated.pinned()).isTrue();
        assertThat(service.list(owner, "", true, 0).content()).extracting(MemoSummaryDto::id).containsExactly(original.id());
        assertThat(service.find(original.id(), owner)).isEqualTo(updated);
    }

    private Member member(String name) {
        String phone = "010" + String.format("%08d", Integer.toUnsignedLong(name.hashCode()) % 100000000);
        return members.saveAndFlush(Member.builder().email(name + "@example.test").name(name).pwd("test-hash")
                .phone(phone).birth("2000-01-01").profile("profile.png").role(Role.USER).build());
    }

    private MemoRequestDto request(String title, String content, boolean pinned, Long version) {
        return new MemoRequestDto(title, content, MemoColor.LAVENDER, pinned, version);
    }
}
