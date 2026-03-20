package spring.study.staticjs;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommentModalRegressionTest {

    private static final Path COMMON_ACTIONS_JS_PATH = Path.of("src/main/resources/static/js/board/common-actions.js");
    private static final Path MEMBER_BOARD_MODAL_JS_PATH = Path.of("src/main/resources/static/js/member/boardModal.js");

    @Test
    void commentEntryPointsShouldOpenModalWhenAvailable() throws IOException {
        assertTrue(Files.readString(COMMON_ACTIONS_JS_PATH).contains("typeof openCommentModal === 'function'"),
                "board common actions should prefer opening the comment modal");
        assertTrue(Files.readString(MEMBER_BOARD_MODAL_JS_PATH).contains("typeof global.openCommentModal === 'function'"),
                "member board modal should prefer opening the comment modal");
    }
}
