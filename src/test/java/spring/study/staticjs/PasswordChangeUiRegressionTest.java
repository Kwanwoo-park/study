package spring.study.staticjs;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordChangeUiRegressionTest {
    @Test
    void authenticatedPasswordSettingsShouldRequireEmailVerification() throws IOException {
        String template = Files.readString(Path.of("src/main/resources/templates/member/updatePassword.html"));
        String script = Files.readString(Path.of("src/main/resources/static/js/member/updatePassword.js"));
        String controller = Files.readString(Path.of(
                "src/main/java/spring/study/member/controller/MemberApiController.java"));

        assertTrue(template.contains("data-verification-required=${emailVerificationRequired}"));
        assertTrue(template.contains("th:if=\"${emailVerificationRequired}\""));
        assertTrue(template.contains("th:disabled=\"${emailVerificationRequired}\""));
        assertTrue(script.contains("/api/member/password-verification/send"));
        assertTrue(script.contains("/api/member/password-verification/verify"));
        assertTrue(script.contains("/api/member/updatePassword/authenticated"));
        assertTrue(controller.contains("passwordChangeVerificationService.consumeVerification(member)"));
    }

    @Test
    void passwordRecoveryShouldKeepItsSeparateUpdateFlow() throws IOException {
        String script = Files.readString(Path.of("src/main/resources/static/js/member/updatePassword.js"));
        String viewController = Files.readString(Path.of(
                "src/main/java/spring/study/member/controller/MemberViewController.java"));

        assertTrue(script.contains(": '/api/member/updatePassword';"));
        assertTrue(viewController.contains("model.addAttribute(\"emailVerificationRequired\", false);"));
        assertTrue(viewController.contains("model.addAttribute(\"emailVerificationRequired\", true);"));
    }
}
