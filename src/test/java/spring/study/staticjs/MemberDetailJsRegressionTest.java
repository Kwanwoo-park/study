package spring.study.staticjs;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MemberDetailJsRegressionTest {

    private static final Path DETAIL_JS_PATH = Path.of("src/main/resources/static/js/member/detail.js");
    private static final Path MEMBER_DETAIL_JS_PATH = Path.of("src/main/resources/static/js/member/memberDetail.js");
    private static final Path BOARD_MODAL_JS_PATH = Path.of("src/main/resources/static/js/member/boardModal.js");

    @Test
    void boardNavigationClickListenersShouldBeRegisteredOnlyOnce() throws IOException {
        assertSingleBoardNavigationListeners(BOARD_MODAL_JS_PATH);
    }

    @Test
    void boardNavigationShouldCallOpenBoardModalOncePerDirection() throws IOException {
        assertSingleBoardNavigationModalCalls(BOARD_MODAL_JS_PATH);
    }

    @Test
    void pageScriptsShouldInitializeSharedBoardModalOnlyOnce() throws IOException {
        assertSingleModalInitializer(DETAIL_JS_PATH);
        assertSingleModalInitializer(MEMBER_DETAIL_JS_PATH);
    }

    private int count(String source, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(source);
        int matches = 0;
        while (matcher.find()) {
            matches++;
        }
        return matches;
    }

    private void assertSingleBoardNavigationListeners(Path scriptPath) throws IOException {
        String script = Files.readString(scriptPath);
        String file = scriptPath.getFileName().toString();

        assertEquals(1, count(script, "boardPrevBtn\\.addEventListener\\('click'"),
                file + ": boardPrevBtn click listener must be registered exactly once");
        assertEquals(1, count(script, "boardNextBtn\\.addEventListener\\('click'"),
                file + ": boardNextBtn click listener must be registered exactly once");
    }

    private void assertSingleBoardNavigationModalCalls(Path scriptPath) throws IOException {
        String script = Files.readString(scriptPath);
        String file = scriptPath.getFileName().toString();

        assertEquals(1, count(script, "openBoardModal\\(currentPrevBoardId, false\\)"),
                file + ": prev navigation should have exactly one modal open call");
        assertEquals(1, count(script, "openBoardModal\\(currentNextBoardId, false\\)"),
                file + ": next navigation should have exactly one modal open call");
    }

    private void assertSingleModalInitializer(Path scriptPath) throws IOException {
        String script = Files.readString(scriptPath);
        String file = scriptPath.getFileName().toString();

        assertEquals(1, count(script, "initMemberBoardModal\\(\\)"),
                file + ": shared modal initializer must be called exactly once");
    }
}
