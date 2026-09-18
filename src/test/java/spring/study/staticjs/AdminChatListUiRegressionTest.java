package spring.study.staticjs;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class AdminChatListUiRegressionTest {
    private static final Path TEMPLATE = Path.of("src/main/resources/templates/chat/adminChatList.html");
    private static final Path CSS = Path.of("src/main/resources/static/css/chat/admin-list.css");

    @Test
    void tableHasItsOwnBoundedAccessibleScrollRegionInsteadOfCenteredChatLayout() throws IOException {
        String template = Files.readString(TEMPLATE);
        assertThat(template).contains("class=\"container admin-chat-list\"", "/css/chat/admin-list.css?v=")
                .doesNotContain("/css/chat/chat.css");
        assertThat(Pattern.compile("<div class=\"admin-chat-table-scroll\"[^>]*>\\s*<table[\\s\\S]*?</table>\\s*</div>")
                .matcher(template).find()).isTrue();
        assertThat(template).contains("role=\"region\"", "aria-labelledby=\"admin-chat-list-title\"",
                "id=\"admin-chat-list-title\"", "tabindex=\"0\"", "th:href=\"@{chatRoom(roomId=${room.roomId})}\"");

        String css = Files.readString(CSS);
        assertThat(rule(css, ".admin-chat-list")).contains("align-items: stretch;", "min-width: 0;");
        assertThat(rule(css, ".admin-chat-table-scroll")).contains("width: 100%;", "min-width: 0;",
                "max-width: 100%;", "overflow-x: auto;", "flex: 0 0 auto;", "-webkit-overflow-scrolling: touch;");
    }

    @Test
    void longRoomNamesWrapInsideReadableColumnsAndMobileShowsSwipeHint() throws IOException {
        String css = Files.readString(CSS);
        assertThat(rule(css, ".admin-chat-table-scroll .table"))
                .contains("width: 100%;", "min-width: 520px;", "table-layout: fixed;");
        assertThat(rule(css, ".admin-chat-table-scroll td")).contains("overflow-wrap: anywhere;");
        assertThat(css).contains("@media (max-width: 576px)", ".admin-chat-scroll-hint", "display: block;");
        assertThat(Files.readString(TEMPLATE)).contains("class=\"admin-chat-scroll-hint\"");
    }

    private String rule(String css, String selector) {
        var matcher = Pattern.compile(Pattern.quote(selector) + "\\s*\\{([^}]*)}").matcher(css);
        assertThat(matcher.find()).as("CSS rule for %s", selector).isTrue();
        return matcher.group(1);
    }
}
