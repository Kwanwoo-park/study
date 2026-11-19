package spring.study.common.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
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
import spring.study.board.entity.Board;
import spring.study.board.facade.BoardViewFacade;
import spring.study.board.service.BoardService;
import spring.study.comment.controller.CommentViewController;
import spring.study.common.component.MobileViewModelAdvice;
import spring.study.common.service.JwtManager;
import spring.study.common.service.VisibilityAccessPolicy;
import spring.study.favorite.controller.FavoriteViewController;
import spring.study.favorite.service.FavoriteService;
import spring.study.follow.controller.FollowViewController;
import spring.study.follow.service.FollowService;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

    @Configuration
    @EnableWebMvc
    @Import({BoardViewController.class, BoardViewFacade.class, CommentViewController.class,
            FavoriteViewController.class, FollowViewController.class, MobileViewModelAdvice.class})
    static class Config {
        @Bean JwtManager jwtManager() { return mock(JwtManager.class); }
        @Bean BoardService boards() { return mock(BoardService.class); }
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
