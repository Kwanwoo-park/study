package spring.study.regression;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoardAccessRegressionTest {
    private static final Path BOARD_VIEW_CONTROLLER = Path.of("src/main/java/spring/study/board/controller/BoardViewController.java");

    @Test
    void boardAllShouldUseSessionRoleCheckInsteadOfPreAuthorize() throws IOException {
        String controller = Files.readString(BOARD_VIEW_CONTROLLER);

        assertFalse(controller.contains("@PreAuthorize"),
                "board/all should not depend on Spring Security method auth because admin pages use SessionManager role checks");
        assertTrue(controller.contains("member.getRole() != Role.ADMIN"),
                "board/all should reject non-admin session members");
        assertTrue(controller.contains("redirect:/member/login?error=true&exception=Wrong Accept"),
                "board/all should match the existing admin-page rejection flow");
    }
}
