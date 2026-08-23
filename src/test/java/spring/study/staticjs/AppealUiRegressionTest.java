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
    void anonymousAppealShouldRequireAccountCredentials() throws IOException {
        String template = read("src/main/resources/templates/appeal/form.html");
        String script = read("src/main/resources/static/js/appeal/form.js");

        assertTrue(template.contains("비로그인 상태에서는 본인 확인을 위해 이메일과 비밀번호가 필요합니다."));
        assertTrue(template.contains("id=\"appealPasswordGroup\""));
        assertTrue(script.contains("(!authenticated && !payload.password)"));
        assertTrue(script.contains("credentials: 'include'"));
    }

    @Test
    void administratorShouldHaveAppealInboxLinkedFromReportPage() throws IOException {
        String reportApply = read("src/main/resources/templates/admin/report_apply.html");
        String adminAppeal = read("src/main/resources/templates/admin/appeal_list.html");
        String adminScript = read("src/main/resources/static/js/admin/appeal.js");

        assertTrue(reportApply.contains("onclick=\"location.href='/admin/appeal'\">상소문</button>"));
        assertTrue(adminAppeal.contains("id=\"adminAppealList\""));
        assertTrue(adminScript.contains("/api/admin/appeal?status=PENDING"));
    }

    private String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }
}
