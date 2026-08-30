package spring.study.staticjs;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppealUiRegressionTest {
    @Test
    void loginAndOwnMemberDetailShouldLinkToAppealForm() throws IOException {
        String login = read("src/main/resources/templates/member/login.html");
        String detail = read("src/main/resources/templates/member/detail.html");

        assertTrue(login.contains("id=\"appealLink\" href=\"/appeal\""));
        assertTrue(detail.contains("onclick=\"location.href='/appeal'\">상소문 작성</button>"));
    }

    @Test
    void authenticatedAppealFormShouldLoadOnlyServerResolvedMemberContext() throws IOException {
        String template = read("src/main/resources/templates/appeal/form.html");
        String script = read("src/main/resources/static/js/appeal/form.js");

        assertTrue(template.contains("th:if=\"${authenticated}\""));
        assertTrue(template.contains("내 신고·제재 내역"));
        assertTrue(script.contains("fetch('/api/appeal/context', { credentials: 'include' })"));
        assertTrue(script.contains("sanctionId: sanctionSelect?.value ? Number(sanctionSelect.value) : null"));
        assertFalse(script.contains("reporter"), "the member appeal UI must not expose reporter identity");
    }

    @Test
    void anonymousAppealShouldRequireFiveMinuteEmailVerificationWithoutPassword() throws IOException {
        String template = read("src/main/resources/templates/appeal/form.html");
        String script = read("src/main/resources/static/js/appeal/form.js");

        assertTrue(template.contains("인증번호와 인증 완료 상태는 각각 5분 동안 유효합니다."));
        assertTrue(template.contains("id=\"appealVerificationSend\""));
        assertTrue(template.contains("id=\"appealVerificationConfirm\""));
        assertTrue(script.contains("/api/appeal/verification/send"));
        assertTrue(script.contains("/api/appeal/verification/verify"));
        assertTrue(script.contains("verificationToken: authenticated ? null : verificationToken"));
        assertTrue(script.contains("Number(data.expiresInSeconds || 300)"));
        assertTrue(script.contains("credentials: 'include'"));
        assertFalse(template.contains("appealPassword"));
        assertFalse(template.contains("type=\"password\""));
        assertFalse(script.contains("payload.password"));
    }

    @Test
    void administratorShouldHaveAppealInboxLinkedFromReportPage() throws IOException {
        String administrator = read("src/main/resources/templates/admin/administrator.html");
        String reportApply = read("src/main/resources/templates/admin/report_apply.html");
        String adminAppeal = read("src/main/resources/templates/admin/appeal_list.html");
        String adminScript = read("src/main/resources/static/js/admin/appeal.js");

        assertTrue(administrator.contains("onclick=\"location.href='/admin/appeal'\">상소문 확인</button>"));
        assertTrue(reportApply.contains("onclick=\"location.href='/admin/appeal'\">상소문</button>"));
        assertTrue(adminAppeal.contains("id=\"adminAppealList\""));
        assertTrue(adminScript.contains("/api/admin/appeal?status=PENDING"));
        assertTrue(adminScript.contains("이메일: ${escapeHtml(item.memberEmail)}"));
        assertTrue(adminScript.contains("data-appeal-unblock"));
        assertTrue(adminScript.contains("/api/admin/appeal/${encodeURIComponent(appealId)}/unblock"));
    }

    private String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }
}
