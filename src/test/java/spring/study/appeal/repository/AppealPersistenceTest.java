package spring.study.appeal.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import spring.study.appeal.entity.Appeal;
import spring.study.appeal.entity.AppealStatus;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.repository.MemberRepository;
import spring.study.member.sanction.entity.MemberSanction;
import spring.study.member.sanction.repository.MemberSanctionRepository;
import spring.study.report.entity.Report;
import spring.study.report.entity.ReportAction;
import spring.study.report.entity.ReportReason;
import spring.study.report.entity.ReportStatus;
import spring.study.report.entity.ReportTargetType;
import spring.study.report.repository.ReportRepository;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.ANY,
        connection = EmbeddedDatabaseConnection.H2
)
class AppealPersistenceTest {
    @Autowired
    private AppealRepository appealRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private ReportRepository reportRepository;
    @Autowired
    private MemberSanctionRepository memberSanctionRepository;

    @Test
    void appealShouldPersistWithItsOwnMemberAndRelatedSanction() {
        Member target = memberRepository.save(member("target@example.com", "01011112222", Role.USER));
        Member reporter = memberRepository.save(member("reporter@example.com", "01033334444", Role.USER));
        Member admin = memberRepository.save(member("admin@example.com", "01055556666", Role.ADMIN));
        Report report = reportRepository.save(Report.builder()
                .reporter(reporter)
                .status(ReportStatus.RESOLVED)
                .targetType(ReportTargetType.MEMBER)
                .targetId(target.getEmail())
                .reason(ReportReason.ABUSE)
                .description("신고 내용")
                .action(ReportAction.WARNING)
                .build());
        MemberSanction sanction = memberSanctionRepository.save(MemberSanction.builder()
                .member(target)
                .report(report)
                .issuedBy(admin)
                .type(ReportAction.WARNING)
                .reason("관리자 처리 사유")
                .startedAt(LocalDateTime.now())
                .build());
        appealRepository.save(Appeal.builder()
                .member(target)
                .relatedSanction(sanction)
                .title("재검토 요청")
                .content("상소 내용")
                .build());

        Appeal saved = appealRepository.findByMemberOrderByRegisterTimeDesc(target).get(0);

        assertThat(saved.getStatus()).isEqualTo(AppealStatus.PENDING);
        assertThat(saved.getRelatedSanction().getReport().getId()).isEqualTo(report.getId());
        assertThat(saved.getMember().getId()).isEqualTo(target.getId());
    }

    private Member member(String email, String phone, Role role) {
        return Member.builder()
                .email(email)
                .pwd("encoded-password")
                .name("member")
                .role(role)
                .phone(phone)
                .birth("20000101")
                .profile("profile.png")
                .build();
    }
}
