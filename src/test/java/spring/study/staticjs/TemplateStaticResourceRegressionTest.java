package spring.study.staticjs;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemplateStaticResourceRegressionTest {

    private static final Path TEMPLATE_ROOT = Path.of("src/main/resources/templates");
    private static final Path BOARD_MAIN_CSS = Path.of("src/main/resources/static/css/board/main.css");
    private static final Path BOARD_VIEW_CSS = Path.of("src/main/resources/static/css/board/view.css");
    private static final Path MEMBER_DETAIL_CSS = Path.of("src/main/resources/static/css/member/member-detail.css");
    private static final Path COMMON_CSS = Path.of("src/main/resources/static/css/common/common.css");
    private static final Path COMMON_ACTIONS_JS = Path.of("src/main/resources/static/js/board/common-actions.js");
    private static final Path BOARD_MAIN_JS = Path.of("src/main/resources/static/js/board/main.js");

    private static final List<Path> THEME_ONLY_TEMPLATES = List.of(
            Path.of("src/main/resources/templates/admin/administrator.html"),
            Path.of("src/main/resources/templates/admin/member_check.html"),
            Path.of("src/main/resources/templates/admin/update_member.html"),
            Path.of("src/main/resources/templates/admin/forbidden_word_list.html"),
            Path.of("src/main/resources/templates/admin/forbidden_word_apply.html"),
            Path.of("src/main/resources/templates/chat/chatRoom.html")
    );

    @Test
    void templatesShouldNotContainInlineStylesOrStyleAttributes() throws IOException {
        try (Stream<Path> templates = Files.walk(TEMPLATE_ROOT)) {
            templates.filter(path -> path.toString().endsWith(".html"))
                    .forEach(this::assertTemplateHasNoInlineCss);
        }
    }

    @Test
    void templatesShouldNotContainInlineDarkModeScript() throws IOException {
        try (Stream<Path> templates = Files.walk(TEMPLATE_ROOT)) {
            templates.filter(path -> path.toString().endsWith(".html"))
                    .forEach(this::assertTemplateHasNoInlineThemeScript);
        }
    }

    @Test
    void themeOnlyPagesShouldLoadThemeScript() throws IOException {
        for (Path templatePath : THEME_ONLY_TEMPLATES) {
            String template = Files.readString(templatePath);

            assertTrue(template.contains("<script src=\"/js/common/theme.js\"></script>"),
                    templatePath + ": should load shared theme script");
        }
    }

    @Test
    void boardImageCssShouldKeepHeightBoundedAndStable() throws IOException {
        String boardMainCss = Files.readString(BOARD_MAIN_CSS);
        String boardViewCss = Files.readString(BOARD_VIEW_CSS);
        String memberDetailCss = Files.readString(MEMBER_DETAIL_CSS);
        String commonCss = Files.readString(COMMON_CSS);

        assertTrue(boardMainCss.contains("height: 100%;"), "feed board images should use height 100%");
        assertTrue(boardMainCss.contains("max-height: 375px;"), "feed board images should be capped at 375px");
        assertTrue(boardViewCss.contains("height: 100%;"), "board detail images should use height 100%");
        assertTrue(boardViewCss.contains("max-height: 375px;"), "board detail images should be capped at 375px");
        assertTrue(memberDetailCss.contains("height: 100%;"), "member board images should use height 100%");
        assertTrue(memberDetailCss.contains("max-height: 375px;"), "member board images should be capped at 375px");
        assertTrue(commonCss.contains("max-height: 375px;"), "mobile board image override should preserve the height cap");
    }

    @Test
    void boardImageNavigationShouldUseClassBasedVisibility() throws IOException {
        String commonActions = Files.readString(COMMON_ACTIONS_JS);
        String boardMain = Files.readString(BOARD_MAIN_JS);

        assertTrue(boardMain.contains("arrow is-invisible"), "initial hidden image arrow should use CSS class");
        assertTrue(commonActions.contains("classList.add('is-invisible')"), "image navigation should hide with CSS class");
        assertTrue(commonActions.contains("classList.remove('is-invisible')"), "image navigation should show with CSS class");
        assertFalse(commonActions.contains("style.visibility"), "image navigation should not depend on inline visibility");
    }

    private void assertTemplateHasNoInlineCss(Path templatePath) {
        try {
            String template = Files.readString(templatePath);

            assertFalse(template.contains("<style"), templatePath + ": should not contain <style> blocks");
            assertFalse(template.contains("style=\""), templatePath + ": should not contain inline style attributes");
        } catch (IOException error) {
            throw new AssertionError("Failed to read " + templatePath, error);
        }
    }

    private void assertTemplateHasNoInlineThemeScript(Path templatePath) {
        try {
            String template = Files.readString(templatePath);

            assertFalse(template.contains("localStorage.getItem('theme')"),
                    templatePath + ": should use /js/common/theme.js instead of inline theme script");
            assertFalse(template.contains("테마 조회 오류"),
                    templatePath + ": should not duplicate theme error handling inline");
        } catch (IOException error) {
            throw new AssertionError("Failed to read " + templatePath, error);
        }
    }
}
