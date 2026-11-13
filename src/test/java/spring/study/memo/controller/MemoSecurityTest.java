package spring.study.memo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
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
import spring.study.memo.config.MemoSecurityConfig;
import spring.study.memo.dto.MemoResponseDto;
import spring.study.memo.entity.MemoColor;
import spring.study.memo.facade.MemoFacade;
import spring.study.admin.facade.SystemIncidentFacade;
import spring.study.memo.service.MemoService;
import spring.study.memo.service.MemoDraftService;
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


import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(MemoSecurityTest.Config.class)
@WebAppConfiguration
class MemoSecurityTest {
    private static final String JSON = "{\"title\":\"메모\",\"content\":\"내용\",\"color\":\"MINT\",\"pinned\":true,\"version\":0}";
    @Autowired WebApplicationContext context;
    @Autowired MemoService service;
    @Autowired MemoDraftService drafts;
    private MockMvc mvc;
    private final Member admin = Member.builder().id(7L).email("admin@example.test").role(Role.ADMIN).build();

    @BeforeEach
    void setUp() {
        reset(service, drafts);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void everyMutationRequiresCsrfEvenForAdministrators() throws Exception {
        mvc.perform(post("/api/memos").with(user(admin)).contentType("application/json").content(JSON)).andExpect(status().isForbidden());
        mvc.perform(put("/api/memos/1").with(user(admin)).contentType("application/json").content(JSON)).andExpect(status().isForbidden());
        mvc.perform(patch("/api/memos/1/settings").with(user(admin)).contentType("application/json").content(JSON)).andExpect(status().isForbidden());
        mvc.perform(patch("/api/memos/1/draft").with(user(admin)).contentType("application/json").content(JSON)).andExpect(status().isForbidden());
        mvc.perform(post("/api/memos/1/reload?version=0").with(user(admin))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/memos/1?version=0").with(user(admin))).andExpect(status().isForbidden());
        verifyNoInteractions(service);

        when(service.find(1L, admin)).thenReturn(new MemoResponseDto(1L, "메모", "내용", MemoColor.LAVENDER, false, 0L, null, null));
        when(service.persist(any())).thenReturn(true);
        mvc.perform(withCsrf(patch("/api/memos/1/settings")).contentType("application/json").content(JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("color").value("MINT"))
                .andExpect(jsonPath("pinned").value(true)).andExpect(jsonPath("version").value(1));
    }

    @Test
    void ordinaryMembersCanUseTheirMemosButAnonymousAndDeniedMembersCannot() throws Exception {
        Member ordinary = Member.builder().id(8L).role(Role.USER).build();
        Member denied = Member.builder().id(9L).role(Role.DENIED).build();
        mvc.perform(get("/api/memos")).andExpect(status().isUnauthorized());
        mvc.perform(withCsrf(patch("/api/memos/1/draft")).with(user(denied)).contentType("application/json").content(JSON)).andExpect(status().isForbidden());
        verifyNoInteractions(service, drafts);

        when(service.find(1L, ordinary)).thenReturn(new MemoResponseDto(1L, "메모", "내용", MemoColor.MINT, false, 0L, null, null));
        mvc.perform(withCsrf(patch("/api/memos/1/draft")).with(user(ordinary)).contentType("application/json").content(JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("pending").value(false));
        verify(service, never()).persist(any());
    }

    private MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder request) throws Exception {
        MockHttpServletResponse response = mvc.perform(get("/api/memos/csrf").with(user(admin))).andExpect(status().isOk()).andReturn().getResponse();
        JsonNode token = new ObjectMapper().readTree(response.getContentAsString());
        return request.with(user(admin)).cookie(response.getCookie("MEMOS-CSRF"))
                .header(token.get("headerName").asText(), token.get("token").asText());
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    @Import({MemoSecurityConfig.class, MemoApiController.class, MemoFacade.class, MemoViewController.class, GlobalExceptionHandler.class})
    static class Config {
        @Bean MemoService memos() { return mock(MemoService.class); }
        @Bean MemoDraftService drafts() { return mock(MemoDraftService.class); }
        @Bean SystemIncidentFacade incidents() { return mock(SystemIncidentFacade.class); }
        @Bean JwtAuthenticationFilter jwtFilter() { return new JwtAuthenticationFilter(mock(JwtTokenProvider.class), mock(MemberService.class), new JwtCookieService(), mock(RefreshTokenService.class), mock(MemberTokenCacheService.class), mock(OnlineUserService.class)); }
        @Bean InternalResourceViewResolver views() { return new InternalResourceViewResolver("/templates/", ".html"); }
    }
}
