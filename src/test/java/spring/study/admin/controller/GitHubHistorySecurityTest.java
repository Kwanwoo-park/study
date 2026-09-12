package spring.study.admin.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import spring.study.admin.config.AdminFileSecurityConfig;
import spring.study.admin.config.GitHubHistoryConfig;
import spring.study.admin.dto.GitHubHistoryResponse;
import spring.study.admin.service.GitHubHistoryService;
import spring.study.common.service.OnlineUserService;
import spring.study.jwt.component.JwtAuthenticationFilter;
import spring.study.jwt.component.JwtTokenProvider;
import spring.study.jwt.service.JwtCookieService;
import spring.study.jwt.service.MemberTokenCacheService;
import spring.study.jwt.service.RefreshTokenService;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(GitHubHistorySecurityTest.Config.class)
@WebAppConfiguration
class GitHubHistorySecurityTest {
    @Autowired WebApplicationContext context;
    @Autowired GitHubHistoryService securedService;
    GitHubHistoryService service;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = AopTestUtils.getUltimateTargetObject(securedService);
        reset(service);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/admin/github/commits", "/api/admin/github/activity", "/api/admin/github/activity?cursor=before:secret"})
    void directApiAccessRequiresAdministrator(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        mvc.perform(get(path).with(user(member(Role.USER)))).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void pageIsHiddenFromAnonymousAndNormalUsersEvenWithAnAdminReferer() throws Exception {
        mvc.perform(get("/admin/github").header("Referer", "https://www.kwanwoo.site/admin/administrator"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/admin/github").with(user(member(Role.USER))))
                .andExpect(status().isNotFound());
        verifyNoInteractions(service);
    }

    @Test
    void adminCanAccessDedicatedPageAndBothReadOnlyApis() throws Exception {
        when(service.commits(1)).thenReturn(new GitHubHistoryResponse<>("Kwanwoo-park/study", "main", List.of(), null, Instant.now()));
        when(service.activity("before:abc")).thenReturn(new GitHubHistoryResponse<>("Kwanwoo-park/study", "main", List.of(), null, Instant.now()));
        mvc.perform(get("/admin/github").with(user(member(Role.ADMIN))))
                .andExpect(status().isOk()).andExpect(view().name("admin/github"))
                .andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get("/api/admin/github/commits").with(user(member(Role.ADMIN))))
                .andExpect(status().isOk()).andExpect(jsonPath("entries").isArray())
                .andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get("/api/admin/github/activity").param("cursor", "before:abc").with(user(member(Role.ADMIN))))
                .andExpect(status().isOk()).andExpect(jsonPath("repository").value("Kwanwoo-park/study"));
        verify(service).commits(1);
        verify(service).activity("before:abc");
    }

    @Test
    void malformedPageIsBadRequestAndNeverCallsGitHub() throws Exception {
        mvc.perform(get("/api/admin/github/commits?page=bad").with(user(member(Role.ADMIN))))
                .andExpect(status().isBadRequest()).andExpect(header().string("Cache-Control", "no-store"));
        verifyNoInteractions(service);
    }

    @Test
    void upstreamFailureIsAJsonNoticeNotAnInternalErrorOrLoginFailure() throws Exception {
        when(service.commits(1)).thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "GitHub 연결 확인"));
        mvc.perform(get("/api/admin/github/commits").with(user(member(Role.ADMIN))))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("message").value("GitHub 연결 확인"))
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    private Member member(Role role) {
        return Member.builder().id(7L).email("admin@example.test").pwd("unused").role(role).build();
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    @Import({GitHubHistoryConfig.class, AdminFileSecurityConfig.class, GitHubHistoryController.class})
    static class Config {
        @Bean
        GitHubHistoryService service() { return mock(GitHubHistoryService.class, withSettings().withoutAnnotations()); }

        @Bean
        JwtAuthenticationFilter jwtFilter() {
            return new JwtAuthenticationFilter(mock(JwtTokenProvider.class), mock(JwtCookieService.class),
                    mock(RefreshTokenService.class), mock(MemberTokenCacheService.class), mock(OnlineUserService.class));
        }

        @Bean
        InternalResourceViewResolver viewResolver() { return new InternalResourceViewResolver("/templates/", ".html"); }
    }
}
