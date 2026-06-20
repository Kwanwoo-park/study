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
    private static final Path COMMON_JS = Path.of("src/main/resources/static/js/common/common.js");
    private static final Path NOTIFICATION_LIST_JS = Path.of("src/main/resources/static/js/notification/list.js");
    private static final Path CHAT_JS = Path.of("src/main/resources/static/js/chat/chat.js");
    private static final Path CHAT_CSS = Path.of("src/main/resources/static/css/chat/chat.css");
    private static final Path COMMON_FRAGMENT = Path.of("src/main/resources/templates/fragments/common.html");

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

        assertTrue(boardMainCss.contains("height: auto;"), "feed board images should keep their intrinsic ratio");
        assertTrue(boardMainCss.contains("max-height: 375px;"), "feed board images should be capped at 375px");
        assertTrue(boardMainCss.contains("object-fit: contain;"), "feed board images should not be cropped");
        assertTrue(boardViewCss.contains("height: auto;"), "board detail images should keep their intrinsic ratio");
        assertTrue(boardViewCss.contains("max-height: 375px;"), "board detail images should be capped at 375px");
        assertTrue(boardViewCss.contains("object-fit: contain;"), "board detail images should not be cropped");
        assertTrue(memberDetailCss.contains("height: auto;"), "member board images should keep their intrinsic ratio");
        assertTrue(memberDetailCss.contains("max-height: 375px;"), "member board images should be capped at 375px");
        assertTrue(memberDetailCss.contains("object-fit: contain;"), "member board images should not be cropped");
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

    @Test
    void chatMessagesShouldRenderTimeAndDateSeparators() throws IOException {
        String chatJs = Files.readString(CHAT_JS);
        String chatCss = Files.readString(CHAT_CSS);

        assertTrue(chatJs.contains("appendMessageTime(newMsgArea, data);"),
                "chat messages should append a visible send time");
        assertTrue(chatJs.contains("refreshDateSeparators();"),
                "chat rendering should refresh date separators after message changes");
        assertTrue(chatJs.contains("messageLi.dataset.messageDate = getMessageDateKey(data);"),
                "chat message rows should keep a date key for separator rendering");
        assertTrue(chatJs.contains("if (mine) {"),
                "chat should only force scroll for the current user's realtime messages");
        assertTrue(chatJs.contains("showNewMessageNotice(data);"),
                "chat should show a new-message notice for other users' realtime messages");
        assertTrue(chatJs.contains("const senderName = data && data.member && data.member.name ? data.member.name : '알 수 없음';"),
                "new-message notice should include the incoming message sender");
        assertTrue(chatJs.contains("newMessageNotice.innerText = `${senderName}: ${message}`;"),
                "new-message notice should show the latest message as sender and message");
        assertTrue(chatJs.contains("newMessageNotice.addEventListener('click'"),
                "new-message notice should scroll to the latest message on click");
        assertTrue(chatJs.contains("activateChatPresence();"),
                "chat room page should mark the current room as active");
        assertTrue(chatJs.contains("deactivateChatPresence();"),
                "chat room page should clear active presence when leaving");
        assertTrue(chatCss.contains(".chat-message-time"),
                "chat message send time should have muted styling");
        assertTrue(chatCss.contains(".chat-date-separator"),
                "chat date separators should be styled");
        assertTrue(chatCss.contains(".chat-new-message-notice"),
                "new-message notice should be styled");
        assertTrue(chatCss.contains("min-height: 44px;"),
                "new-message notice should have a comfortable height");
    }

    @Test
    void notificationPopupCloseShouldMarkNotificationAsRead() throws IOException {
        String commonJs = Files.readString(COMMON_JS);

        assertTrue(commonJs.contains("const notificationId = json['id'];"),
                "SSE notification id should be captured for read updates");
        assertTrue(commonJs.contains("notificationBanner.dataset.notificationId = notificationId || '';"),
                "notification banner should keep the current notification id");
        assertTrue(commonJs.contains("fnMarkNotificationAsRead(notificationId);"),
                "notification close button should mark the notification as read");
        assertTrue(commonJs.contains("fnMoveNotificationAfterRead(notificationId, notificationGroup, notificationUrl);"),
                "notification banner click should mark as read before moving");
    }

    @Test
    void notificationListClickShouldMarkNotificationAsReadBeforeMoving() throws IOException {
        String notificationListJs = Files.readString(NOTIFICATION_LIST_JS);

        assertTrue(notificationListJs.contains("clickDiv.onclick = async function()"),
                "notification item clicks should wait for read handling");
        assertTrue(notificationListJs.contains("await fnReadSilent(item.id);"),
                "notification item click should silently mark the item as read");
        assertTrue(notificationListJs.contains("fnNotificationMove(item.notiGroup, item.url);"),
                "notification item click should continue to the target url");
    }

    @Test
    void notificationListGroupButtonsShouldRenderUnreadCounts() throws IOException {
        String notificationListJs = Files.readString(NOTIFICATION_LIST_JS);
        String commonJs = Files.readString(COMMON_JS);

        assertTrue(notificationListJs.contains("notificationGroupButtons"),
                "notification list should define group button labels");
        assertTrue(notificationListJs.contains("fnUpdateGroupUnreadCounts(data.list);"),
                "notification list should compute unread counts from the full list");
        assertTrue(notificationListJs.contains("item.readStatus == 'UNREAD'"),
                "notification group counts should include only unread notifications");
        assertTrue(notificationListJs.contains("button.innerText = `${buttonInfo.label}(${notificationGroupUnreadCounts[group] || 0})`;"),
                "notification group buttons should include unread counts in their labels");
        assertTrue(notificationListJs.contains("fnApplyNotificationReadState(id);"),
                "manual read updates should refresh group counts");
        assertTrue(notificationListJs.contains("fnHandleIncomingNotificationCount(notification)"),
                "notification list should expose a hook for incoming SSE count updates");
        assertTrue(commonJs.contains("fnHandleIncomingNotificationCount(json)"),
                "common notification SSE handler should update notification list group counts when available");
    }

    @Test
    void notificationNavShouldRenderUnreadCountBadge() throws IOException {
        String commonFragment = Files.readString(COMMON_FRAGMENT);
        String commonCss = Files.readString(COMMON_CSS);
        String commonJs = Files.readString(COMMON_JS);

        assertTrue(commonFragment.contains("id=\"notification-unread-count\""),
                "notification nav should include a badge element for unread count");
        assertTrue(commonCss.contains(".notification-count-badge"),
                "notification unread count should be styled as a badge");
        assertTrue(commonCss.contains("min-width: 20px;"),
                "notification unread count badge should be larger than the previous red dot");
        assertTrue(commonJs.contains("notificationCountBadge.textContent"),
                "notification unread count should be rendered by common js");
        assertTrue(commonJs.contains("count > 99 ? '99+' : String(count)"),
                "notification unread count badge should keep long counts compact");
        assertTrue(commonJs.contains("fnSetUnreadNotificationDot(json['count']);"),
                "notification unread count should use the API count value");
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
