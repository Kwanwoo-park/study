package spring.study.memo.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import spring.study.memo.dto.AdminMemoPageDto;
import spring.study.memo.dto.AdminMemoRequestDto;
import spring.study.memo.dto.AdminMemoResponseDto;
import spring.study.memo.dto.AdminMemoSummaryDto;
import spring.study.memo.entity.AdminMemoColor;
import spring.study.memo.service.AdminMemoService;
import spring.study.common.exception.BusinessStateException;
import spring.study.common.exception.ResourceNotFoundException;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.repository.MemberRepository;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY, connection = EmbeddedDatabaseConnection.H2)
@Import(AdminMemoService.class)
class AdminMemoPersistenceTest {
    @Autowired AdminMemoService service;
    @Autowired AdminMemoRepository memos;
    @Autowired MemberRepository members;

    @Test
    void searchesTitleAndBodyLiterallyAndKeepsEveryResultOwnerScoped() {
        Member owner = administrator("owner");
        Member other = administrator("other");
        service.create(request("Release checklist", "준비", false, null), owner);
        service.create(request("비용", "부하 100%_완료!", true, null), owner);
        service.create(request("private", "Release checklist 100%_완료!", true, null), other);

        assertThat(service.list(owner, "release", false, 0).content()).extracting(AdminMemoSummaryDto::title).containsExactly("Release checklist");
        assertThat(service.list(owner, "%_", false, 0).content()).extracting(AdminMemoSummaryDto::title).containsExactly("비용");
        assertThat(service.list(owner, "", true, 0).content()).extracting(AdminMemoSummaryDto::title).containsExactly("비용");
        assertThat(service.list(owner, "", false, 0).content()).extracting(AdminMemoSummaryDto::title).containsExactly("비용", "Release checklist");
        assertThat(service.list(owner, "", false, 0).totalCount()).isEqualTo(2);
    }

    @Test
    void otherAdministratorsCannotReadEditOrDeleteAMemoByGuessingItsId() {
        Member owner = administrator("owner");
        Member other = administrator("other");
        AdminMemoResponseDto memo = service.create(request("비공개", "원본", false, null), owner);

        assertThatThrownBy(() -> service.find(memo.id(), other)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.update(memo.id(), request("변경", "내용", false, memo.version()), other)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.delete(memo.id(), memo.version(), other)).isInstanceOf(ResourceNotFoundException.class);
        assertThat(memos.findById(memo.id()).orElseThrow().getContent()).isEqualTo("원본");
    }

    @Test
    void staleEditsAndDeletesCannotOverwriteANewerVersion() {
        Member owner = administrator("owner");
        AdminMemoResponseDto original = service.create(request("처음", "원본", false, null), owner);
        AdminMemoResponseDto updated = service.update(original.id(), request("수정", "최신 내용", true, original.version()), owner);

        assertThat(updated.version()).isGreaterThan(original.version());
        assertThatThrownBy(() -> service.update(original.id(), request("덮어쓰기", "과거 내용", false, original.version()), owner)).isInstanceOf(BusinessStateException.class);
        assertThatThrownBy(() -> service.delete(original.id(), original.version(), owner)).isInstanceOf(BusinessStateException.class);
        assertThat(service.find(original.id(), owner).content()).isEqualTo("최신 내용");
    }

    private Member administrator(String name) {
        String phone = "010" + String.format("%08d", Integer.toUnsignedLong(name.hashCode()) % 100000000);
        return members.saveAndFlush(Member.builder().email(name + "@example.test").name(name).pwd("test-hash")
                .phone(phone).birth("2000-01-01").profile("profile.png").role(Role.ADMIN).build());
    }

    private AdminMemoRequestDto request(String title, String content, boolean pinned, Long version) {
        return new AdminMemoRequestDto(title, content, AdminMemoColor.LAVENDER, pinned, version);
    }
}
