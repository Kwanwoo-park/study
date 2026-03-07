package spring.study.staticjs;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MemberDetailJsRegressionTest {

    private static final Path DETAIL_JS_PATH =
            Path.of("src/main/resources/static/js/member/detail.js");

    @Test
    void boardNavigationClickListenersShouldBeRegisteredOnlyOnce() throws IOException {
        String script = Files.readString(DETAIL_JS_PATH);

        assertEquals(1, count(script, "boardPrevBtn\\.addEventListener\\('click'"),
                "boardPrevBtn click listener must be registered exactly once");
        assertEquals(1, count(script, "boardNextBtn\\.addEventListener\\('click'"),
                "boardNextBtn click listener must be registered exactly once");
    }

    @Test
    void boardNavigationShouldCallOpenBoardModalOncePerDirection() throws IOException {
        String script = Files.readString(DETAIL_JS_PATH);

        assertEquals(1, count(script, "openBoardModal\\(currentPrevBoardId, false\\)"),
                "prev navigation should have exactly one modal open call");
        assertEquals(1, count(script, "openBoardModal\\(currentNextBoardId, false\\)"),
                "next navigation should have exactly one modal open call");
    }

    private int count(String source, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(source);
        int matches = 0;
        while (matcher.find()) {
            matches++;
        }
        return matches;
    }
}
