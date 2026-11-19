package spring.study.memo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import spring.study.memo.config.MemoSecurityConfig;
import spring.study.memo.dto.MemoResponseDto;
import spring.study.memo.dto.MemoPageDto;
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
import spring.study.memo.dto.MemoDraft;

import java.time.Duration;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(MemoSecurityTest.Config.class)
@WebAppConfiguration
@TestPropertySource(properties = {"security.jwt.secret=memo-tests-secret-at-least-thirty-two-bytes", "security.jwt.issuer=memo-tests", "security.jwt.access-token-minutes=15", "security.jwt.refresh-token-days=14"})
class MemoSecurityTest {
    private static final String JSON = "{\"title\":\"메모\",\"content\":\"내용\",\"color\":\"MINT\",\"pinned\":true,\"version\":0}";
    @Autowired WebApplicationContext context;
    @Autowired MemoService service;
    @Autowired MemoDraftService drafts;
    @Autowired JwtTokenProvider tokenProvider;
    @Autowired MemberTokenCacheService members;
    private MockMvc mvc;
    private final Member admin = Member.builder().id(7L).email("admin@example.test").role(Role.ADMIN).build();

    @BeforeEach
    void setUp() {
        reset(service, drafts, members);
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
        mvc.perform(withCsrf(patch("/api/memos/1/draft")).with(user(denied)).contentType("application/json").content(JSON))
                .andExpect(status().isForbidden()).andExpect(jsonPath("code").value("FORBIDDEN"));
        verifyNoInteractions(service, drafts);

        when(service.find(1L, ordinary)).thenReturn(new MemoResponseDto(1L, "메모", "내용", MemoColor.MINT, false, 0L, null, null));
        mvc.perform(withCsrf(patch("/api/memos/1/draft")).with(user(ordinary)).contentType("application/json").content(JSON))
                .andExpect(status().isOk()).andExpect(jsonPath("pending").value(false));
        verify(service, never()).persist(any());
    }

    @Test
    void listAcceptsAnOptionalColorAndRejectsUnknownColors() throws Exception {
        when(service.list(eq(admin), anyString(), anyBoolean(), nullable(MemoColor.class), anyInt()))
                .thenReturn(new MemoPageDto(List.of(), 0, 0, 0, 0, 0));
        mvc.perform(get("/api/memos").with(user(admin)).param("color", "MINT").param("query", "note")
                        .param("pinnedOnly", "true").param("page", "2"))
                .andExpect(status().isOk());
        verify(service).list(admin, "note", true, MemoColor.MINT, 2);
        mvc.perform(get("/api/memos").with(user(admin))).andExpect(status().isOk());
        verify(service).list(admin, "", false, null, 0);
        mvc.perform(get("/api/memos").with(user(admin)).param("color", "UNKNOWN"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void realJwtRequestsKeepCsrfCookieAcrossRepeatedAutosavesAndReads() throws Exception {
        Member member = Member.builder().id(1L).email("memo@example.test").role(Role.USER).build();
        when(members.findOrLoad(eq(member.getId()), any(Duration.class))).thenReturn(Optional.of(member));
        Cookie auth = new Cookie(JwtCookieService.ACCESS_COOKIE, tokenProvider.createAccessToken(member).value());
        AtomicReference<MemoDraft> draft = new AtomicReference<>(MemoDraft.persisted(new MemoResponseDto(5L, "메모", "원본", MemoColor.MINT, false, 0L, null, null), member.getId()));
        when(drafts.find(5L)).thenAnswer(invocation -> Optional.of(draft.get()));
        when(drafts.lock(5L)).thenReturn("memo-test-lock");
        doAnswer(invocation -> { draft.set(invocation.getArgument(0)); return null; }).when(drafts).store(any(), anyString());

        MockHttpServletResponse csrfResponse = mvc.perform(get("/api/memos/csrf").secure(true).cookie(auth))
                .andExpect(status().isOk()).andExpect(cookie().httpOnly("MEMOS-CSRF", true)).andReturn().getResponse();
        Cookie csrf = csrfResponse.getCookie("MEMOS-CSRF");
        JsonNode token = new ObjectMapper().readTree(csrfResponse.getContentAsString());
        for (int version = 0; version < 3; version++) {
            String payload = "{\"title\":\"메모\",\"content\":\"저장 " + version + "\",\"version\":" + version + "}";
            mvc.perform(patch("/api/memos/5/draft").secure(true).cookie(auth, csrf).header(token.get("headerName").asText(), token.get("token").asText()).contentType("application/json").content(payload))
                    .andExpect(status().isOk()).andExpect(jsonPath("version").value(version + 1)).andExpect(cookie().doesNotExist("MEMOS-CSRF"));
            mvc.perform(get("/api/memos/5/status").secure(true).cookie(auth, csrf))
                    .andExpect(status().isOk()).andExpect(cookie().doesNotExist("MEMOS-CSRF"));
        }
        verify(drafts, times(3)).store(any(), anyString());
    }

    @Test
    void csrfFailuresAreIdentifiableAndDoNotReachStorageEvenWithValidJwt() throws Exception {
        Member member = Member.builder().id(1L).email("memo@example.test").role(Role.USER).build();
        when(members.findOrLoad(eq(member.getId()), any(Duration.class))).thenReturn(Optional.of(member));
        Cookie auth = new Cookie(JwtCookieService.ACCESS_COOKIE, tokenProvider.createAccessToken(member).value());
        MockHttpServletResponse csrfResponse = mvc.perform(get("/api/memos/csrf").cookie(auth)).andExpect(status().isOk()).andReturn().getResponse();
        JsonNode token = new ObjectMapper().readTree(csrfResponse.getContentAsString());
        for (Cookie invalid : new Cookie[]{new Cookie("MEMOS-CSRF", UUID.randomUUID().toString()), new Cookie("unrelated", "no-csrf-cookie")}) {
            mvc.perform(patch("/api/memos/5/draft").cookie(auth, invalid).header(token.get("headerName").asText(), token.get("token").asText()).contentType("application/json").content(JSON))
                    .andExpect(status().isForbidden()).andExpect(content().contentTypeCompatibleWith("application/json"))
                    .andExpect(header().string("Cache-Control", "no-store")).andExpect(jsonPath("code").value("CSRF_INVALID"));
        }
        verifyNoInteractions(service, drafts);
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
        @Bean JwtTokenProvider tokenProvider() { return new JwtTokenProvider(new ObjectMapper()); }
        @Bean MemberTokenCacheService members() { return mock(MemberTokenCacheService.class); }
        @Bean JwtAuthenticationFilter jwtFilter(JwtTokenProvider provider, MemberTokenCacheService members) { return new JwtAuthenticationFilter(provider, mock(MemberService.class), new JwtCookieService(), mock(RefreshTokenService.class), members, mock(OnlineUserService.class)); }
        @Bean InternalResourceViewResolver views() { return new InternalResourceViewResolver("/templates/", ".html"); }
    }
}
