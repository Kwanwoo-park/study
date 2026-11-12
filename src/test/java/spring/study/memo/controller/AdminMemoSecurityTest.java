package spring.study.memo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import spring.study.memo.config.AdminMemoSecurityConfig;
import spring.study.memo.dto.AdminMemoPageDto;
import spring.study.memo.dto.AdminMemoResponseDto;
import spring.study.memo.entity.AdminMemoColor;
import spring.study.memo.facade.AdminMemoFacade;
import spring.study.admin.facade.SystemIncidentFacade;
import spring.study.memo.service.AdminMemoService;
import spring.study.common.component.GlobalExceptionHandler;
import spring.study.common.service.OnlineUserService;
import spring.study.jwt.component.JwtAuthenticationFilter;
import spring.study.jwt.component.JwtTokenProvider;
import spring.study.jwt.service.JwtCookieService;
import spring.study.jwt.service.MemberTokenCacheService;
import spring.study.jwt.service.RefreshTokenService;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.service.MemberService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(AdminMemoSecurityTest.Config.class)
@WebAppConfiguration
class AdminMemoSecurityTest {
    private static final String JSON = "{\"title\":\"메모\",\"content\":\"내용\",\"color\":\"MINT\",\"pinned\":true,\"version\":0}";
    @Autowired WebApplicationContext context;
    @Autowired AdminMemoService service;
    private MockMvc mvc;
    private final Member admin = Member.builder().id(7L).email("admin@example.test").role(Role.ADMIN).build();

    @BeforeEach
    void setUp() {
        reset(service);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void everyMutationRequiresCsrfEvenForAdministrators() throws Exception {
        mvc.perform(post("/api/admin/memos").with(user(admin)).contentType("application/json").content(JSON)).andExpect(status().isForbidden());
        mvc.perform(put("/api/admin/memos/1").with(user(admin)).contentType("application/json").content(JSON)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/admin/memos/1?version=0").with(user(admin))).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void ordinaryMembersCannotWriteEvenWithValidCsrfTokens() throws Exception {
        Member ordinary = Member.builder().id(8L).role(Role.USER).build();
        mvc.perform(withCsrf(post("/api/admin/memos")).with(user(ordinary)).contentType("application/json").content(JSON)).andExpect(status().isForbidden());
        mvc.perform(withCsrf(put("/api/admin/memos/1")).with(user(ordinary)).contentType("application/json").content(JSON)).andExpect(status().isForbidden());
        mvc.perform(withCsrf(delete("/api/admin/memos/1?version=0")).with(user(ordinary))).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    private MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder request) throws Exception {
        MockHttpServletResponse response = mvc.perform(get("/api/admin/memos/csrf").with(user(admin))).andExpect(status().isOk()).andReturn().getResponse();
        JsonNode token = new ObjectMapper().readTree(response.getContentAsString());
        return request.with(user(admin)).cookie(response.getCookie("ADMIN-MEMOS-CSRF"))
                .header(token.get("headerName").asText(), token.get("token").asText());
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    @Import({AdminMemoSecurityConfig.class, AdminMemoController.class, AdminMemoFacade.class, MemoViewController.class, GlobalExceptionHandler.class})
    static class Config {
        @Bean AdminMemoService memos() { return mock(AdminMemoService.class); }
        @Bean SystemIncidentFacade incidents() { return mock(SystemIncidentFacade.class); }
        @Bean JwtAuthenticationFilter jwtFilter() { return new JwtAuthenticationFilter(mock(JwtTokenProvider.class), mock(MemberService.class), new JwtCookieService(), mock(RefreshTokenService.class), mock(MemberTokenCacheService.class), mock(OnlineUserService.class)); }
        @Bean InternalResourceViewResolver views() { return new InternalResourceViewResolver("/templates/", ".html"); }
    }
}
