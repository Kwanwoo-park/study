package spring.study.common.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.thymeleaf.extras.springsecurity6.dialect.SpringSecurityDialect;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import spring.study.board.controller.BoardViewController;
import spring.study.board.dto.BoardBookmarkDto;
import spring.study.board.entity.Board;
import spring.study.board.facade.BoardBookmarkFacade;
import spring.study.board.facade.BoardViewFacade;
import spring.study.board.service.BoardBookmarkService;
import spring.study.board.service.BoardService;
import spring.study.comment.controller.CommentViewController;
import spring.study.common.component.MobileViewModelAdvice;
import spring.study.common.service.JwtManager;
import spring.study.common.service.VisibilityAccessPolicy;
import spring.study.favorite.controller.FavoriteViewController;
import spring.study.favorite.service.FavoriteService;
import spring.study.follow.controller.FollowViewController;
import spring.study.follow.service.FollowService;
import spring.study.diary.controller.DiaryViewController;
import spring.study.diary.facade.DiaryFacade;
import spring.study.diary.service.DiaryService;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.todo.controller.TodoViewController;
import spring.study.todo.dto.TodoResponseDto;
import spring.study.todo.facade.WeekReviewFacade;
import spring.study.todo.service.TodoService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(StandalonePageViewTest.Config.class)
@WebAppConfiguration
class StandalonePageViewTest {
    @Autowired WebApplicationContext context;
    @Autowired JwtManager jwtManager;
    @Autowired BoardService boards;
    @Autowired VisibilityAccessPolicy visibility;
    @Autowired BoardBookmarkService bookmarks;
    @Autowired TodoService todos;
    @Autowired DiaryService diaries;

    @Test
    void directPagesRenderTheViewersNavigationThemeAndContentEvenForAnotherMembersPage() throws Exception {
        Member viewer = Member.builder().id(1L).email("viewer@example.test").profile("/img/viewer.png").role(Role.USER).build();
        Member author = Member.builder().id(2L).email("author@example.test").profile("/img/author.png").name("작성자").build();
        Board board = Board.builder().id(42L).member(author).content("게시글 본문\n다크모드에서도 읽을 수 있는 내용").build();
        when(jwtManager.getLoginMember(any())).thenReturn(viewer);
        when(boards.existBoard(42L)).thenReturn(true);
        when(boards.findById(42L)).thenReturn(board);
        when(boards.getBoardIdList(42L, author, false)).thenReturn(new long[]{0, 0});
        when(visibility.canViewBoard(board, viewer, false)).thenReturn(true);
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
        Path output = Path.of("build", "standalone-pages");
        Files.createDirectories(output);

        Map<String, String> routes = Map.of("board", "/board/view?id=42", "comment", "/comment?id=42",
                "favorite", "/favorites?id=42", "follower", "/follow/follower?email=author@example.test",
                "following", "/follow/following?email=author@example.test");
        for (Map.Entry<String, String> route : routes.entrySet()) {
            MvcResult result = mvc.perform(get(route.getValue()).header("User-Agent", "iPhone Mobile"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("email", viewer.getEmail()))
                    .andExpect(model().attribute("profile", viewer.getProfile()))
                    .andExpect(content().string(containsString("class=\"navi\"")))
                    .andExpect(content().string(containsString("/js/common/theme.js")))
                    .andExpect(content().string(containsString("/js/common/common.js")))
                    .andReturn();
            String html = result.getResponse().getContentAsString();
            int navigationStart = html.indexOf("<nav class=\"navi\">");
            assertThat(html.substring(navigationStart, html.indexOf("</nav>", navigationStart)))
                    .contains(viewer.getProfile(), "profile-icon").doesNotContain(author.getProfile());
            Files.writeString(output.resolve(route.getKey() + ".html"), html);
        }
    }

    @Test
    void bookmarkAndWeekViewsRenderForTheViewerAndDraftsUseTheViewersId() throws Exception {
        Member viewer = Member.builder().id(7L).email("weekly@example.test").profile("/img/viewer.png").role(Role.USER).build();
        when(jwtManager.getLoginMember(any())).thenReturn(viewer);
        when(bookmarks.list(viewer, 0)).thenReturn(new PageImpl<>(List.of(new BoardBookmarkDto(42L, "작성자", "저장한 글"))));
        LocalDateTime today = LocalDateTime.now();
        when(todos.findCompletedForWeek(eq(viewer), any(), any())).thenReturn(new PageImpl<>(List.of(
                new TodoResponseDto(1L, "이번 주 완료", true, today, today, null))));
        when(diaries.findForWeek(eq(viewer), any(), any())).thenReturn(Page.empty());
        MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();

        mvc.perform(get("/board/bookmarks")).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().string(containsString("저장한 글")));
        mvc.perform(get("/todo/week")).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().string(containsString("이번 주 완료")));
        mvc.perform(get("/board/write")).andExpect(status().isOk())
                .andExpect(content().string(containsString("data-draft-key=\"board:7\"")));
        mvc.perform(get("/diary/write")).andExpect(status().isOk())
                .andExpect(content().string(containsString("data-draft-key=\"diary:7:new\"")));
    }

    @Configuration
    @EnableWebMvc
    @Import({BoardViewController.class, BoardViewFacade.class, BoardBookmarkFacade.class, CommentViewController.class,
            FavoriteViewController.class, FollowViewController.class, MobileViewModelAdvice.class,
            TodoViewController.class, WeekReviewFacade.class, DiaryViewController.class})
    static class Config {
        @Bean JwtManager jwtManager() { return mock(JwtManager.class); }
        @Bean BoardService boards() { return mock(BoardService.class); }
        @Bean BoardBookmarkService bookmarks() { return mock(BoardBookmarkService.class); }
        @Bean TodoService todos() { return mock(TodoService.class); }
        @Bean DiaryService diaries() { return mock(DiaryService.class); }
        @Bean DiaryFacade diaryFacade() { return mock(DiaryFacade.class); }
        @Bean FavoriteService favorites() { return mock(FavoriteService.class); }
        @Bean FollowService follows() { return mock(FollowService.class); }
        @Bean VisibilityAccessPolicy visibility() { return mock(VisibilityAccessPolicy.class); }

        @Bean SpringTemplateEngine templateEngine(ApplicationContext context) {
            SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
            resolver.setApplicationContext(context);
            resolver.setPrefix("classpath:/templates/");
            resolver.setSuffix(".html");
            resolver.setCharacterEncoding("UTF-8");
            SpringTemplateEngine engine = new SpringTemplateEngine();
            engine.setTemplateResolver(resolver);
            engine.addDialect(new SpringSecurityDialect());
            return engine;
        }

        @Bean ThymeleafViewResolver viewResolver(SpringTemplateEngine engine) {
            ThymeleafViewResolver resolver = new ThymeleafViewResolver();
            resolver.setTemplateEngine(engine);
            resolver.setCharacterEncoding("UTF-8");
            return resolver;
        }
    }
}
