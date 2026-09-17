package spring.study.staticjs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import spring.study.board.entity.Board;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class PageBackNavigationRegressionTest {
    private static final Path TEMPLATES = Path.of("src/main/resources/templates");
    private static final Map<String, String> ADDED_PAGES = Map.ofEntries(
            Map.entry("admin/administrator", "/board/main"),
            Map.entry("admin/files", "/admin/administrator"),
            Map.entry("admin/forbidden_word_apply", "/admin/administrator"),
            Map.entry("admin/forbidden_word_list", "/admin/administrator"),
            Map.entry("admin/github", "/admin/administrator"),
            Map.entry("admin/report_history", "/admin/administrator"),
            Map.entry("admin/report_process", "/admin/administrator"),
            Map.entry("board/list", "/admin/administrator"),
            Map.entry("board/view", "/board/main"),
            Map.entry("chat/chatRoom", "/chat/chatList"),
            Map.entry("comment/list", "/board/view?id=42"),
            Map.entry("favorite/list", "/board/view?id=42"),
            Map.entry("error/404", "/board/main"),
            Map.entry("member/find", "/member/login"),
            Map.entry("member/login", "/portfolio/"),
            Map.entry("member/updatePhone", "/board/main")
    );

    static Stream<String> addedPages() {
        return ADDED_PAGES.keySet().stream().sorted();
    }

    @ParameterizedTest
    @MethodSource("addedPages")
    void eachChangedPageRendersExactlyOneWorkingBackLink(String page) {
        var resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        var engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        var servletContext = new MockServletContext();
        var request = new MockHttpServletRequest(servletContext);
        var exchange = JakartaServletWebApplication.buildApplication(servletContext)
                .buildExchange(request, new MockHttpServletResponse());
        var context = new WebContext(exchange, Locale.KOREAN, sampleModel(page));

        String html = engine.process(page, context);

        var anchors = Pattern.compile("<a\\b[^>]*\\bdata-page-back(?:=\"[^\"]*\")?[^>]*>").matcher(html).results().toList();
        assertThat(anchors).hasSize(1);
        assertThat(anchors.get(0).group()).contains("href=\"" + ADDED_PAGES.get(page) + "\"");
        assertThat(html).contains("뒤로 가기", "/css/common/page-back.css?v=20260916", "/js/common/page-back.js?v=20260916-replace");
        assertThat(html).doesNotContain("th:replace=\"~{fragments/common :: pageBack");
    }

    @Test
    void everyStandaloneTemplateHasBottomNavigationOrAPageBackControl() throws Exception {
        // The root template redirects immediately; layouts and modal fragments are not standalone pages.
        try (var paths = Files.walk(TEMPLATES)) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".html")).toList()) {
                String relative = TEMPLATES.relativize(path).toString();
                if (relative.startsWith("fragments/") || relative.startsWith("layout/") || relative.equals("index.html")) continue;
                String template = Files.readString(path);
                boolean navigable = template.contains("memberNavi(") || template.contains("pageBack(")
                        || template.contains("history.back(") || template.contains("id=\"appealBack\"")
                        || relative.equals("admin/member_check.html") && template.contains(">Previous</button>");
                assertThat(navigable).as(relative + " requires bottom navigation or a back control").isTrue();
                if (template.contains("memberNavi(")) {
                    assertThat(template).as(relative + " already has bottom navigation").doesNotContain("pageBack(");
                }
            }
        }
    }

    @Test
    void staticPortfolioHasASeparateBackLinkNotJustPreviousSlide() throws Exception {
        String html = Files.readString(Path.of("src/main/resources/static/portfolio/index.html"));
        assertThat(html).contains("href=\"/member/login\" data-page-back", "뒤로 가기", "id=\"prevBtn\"",
                "/css/common/page-back.css?v=20260916", "/js/common/page-back.js?v=20260916-replace");
    }

    @Test
    void chatBackNavigationDoesNotQuitTheRoomAndExistingSpecialBackControlsRemain() throws Exception {
        String chat = Files.readString(TEMPLATES.resolve("chat/chatRoom.html"));
        assertThat(chat).contains("pageBack('/chat/chatList')", "onclick=\"quit()\"");
        String header = chat.substring(chat.indexOf("<header class=\"chat-room-header\""), chat.indexOf("</header>"));
        assertThat(header).contains("pageBack(").doesNotContain("quit()");
        assertThat(Files.readString(TEMPLATES.resolve("member/member_detail.html")))
                .contains("id=\"chatMemberDetailBack\"", "th:if=\"${chatEntry}\"");
        assertThat(Files.readString(TEMPLATES.resolve("appeal/form.html"))).contains("id=\"appealBack\"");
    }

    @Test
    void sharedStylesAreThemedKeyboardAccessibleAndDoNotCoverPageContent() throws Exception {
        String css = Files.readString(Path.of("src/main/resources/static/css/common/page-back.css"));
        assertThat(css).contains("var(--surface-bg", "var(--text-primary", ":focus-visible", "min-height: 44px")
                .doesNotContain("position: fixed", "z-index: 9999");
    }

    private Map<String, Object> sampleModel(String page) {
        Map<String, Object> model = new HashMap<>();
        Member member = Member.builder().id(1L).email("member@example.test").name("회원").profile("/profile.png").role(Role.ADMIN).build();
        model.put("isMobile", false);
        model.put("member", page.equals("board/list") ? member : member.getEmail());
        model.put("email", member.getEmail());
        model.put("list", List.of());
        model.put("participantNames", List.of("회원", "상대 회원"));
        model.put("audioCallAvailable", true);
        model.put("room", "room-1");
        model.put("isAdmin", false);
        model.put("flag", false);
        model.put("previous", 0);
        model.put("next", 0);
        model.put("like", false);
        model.put("boardId", 42L);
        model.put("board", Board.builder().id(42L).member(member).content("게시글").build());
        model.put("resultMap", Map.of("list", List.of(), "totalPage", 1, "totalCnt", 0L, "paging", PageRequest.of(0, 5)));
        model.put("phone", "01000000000");
        model.put("birth", "2000-01-01");
        return model;
    }
}
