package spring.study.admin.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import spring.study.admin.config.IpBlockSecurityConfig;
import spring.study.admin.entity.BlockedIp;
import spring.study.admin.facade.IpBlockFacade;
import spring.study.admin.service.IpBlockService;
import spring.study.common.component.IpBlockClientAddress;
import spring.study.common.service.OnlineUserService;
import spring.study.jwt.component.JwtAuthenticationFilter;
import spring.study.jwt.component.JwtTokenProvider;
import spring.study.jwt.component.RefreshTokenRotation;
import spring.study.jwt.service.JwtCookieService;
import spring.study.jwt.service.MemberTokenCacheService;
import spring.study.jwt.service.RefreshTokenService;
import spring.study.mail.entity.EmailVerificationAttempt;
import spring.study.mail.service.EmailVerificationService;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.service.MemberService;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(IpBlockSecurityTest.Config.class)
@WebAppConfiguration
class IpBlockSecurityTest {
    private static final String PAGE = "/admin/ip-blocks";
    private static final String BLOCK_EMAIL = PAGE + "/email";

    @Autowired WebApplicationContext context;
    @Autowired IpBlockService blocks;
    @Autowired EmailVerificationService emailVerificationService;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        reset(blocks, emailVerificationService);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void onlyAdministratorWithCsrfCanCreateBlocks() throws Exception {
        mvc.perform(get(PAGE)).andExpect(status().isUnauthorized());
        mvc.perform(withActualCsrf(post(PAGE).with(user(member(Role.USER))).param("ipAddress", "203.0.113.12")))
                .andExpect(status().isForbidden());
        mvc.perform(post(PAGE).with(user(member(Role.ADMIN))).param("ipAddress", "203.0.113.12"))
                .andExpect(status().isForbidden());
        verify(blocks, never()).block(anyString(), anyLong());

        var admin = user(member(Role.ADMIN));
        mvc.perform(withActualCsrf(post(PAGE).with(admin).param("ipAddress", "::ffff:203.0.113.12")))
                .andExpect(redirectedUrl(PAGE));
        verify(blocks).block("203.0.113.12", 7L);

        mvc.perform(withActualCsrf(delete(PAGE).with(admin))).andExpect(status().isMethodNotAllowed());
    }

    @Test
    void rejectsSelfBlockAndInputsOtherThanAnIpAddress() throws Exception {
        var admin = user(member(Role.ADMIN));
        for (String value : List.of("127.0.0.1", "example.test", "203.0.113.0/24")) {
            mvc.perform(withActualCsrf(post(PAGE).with(admin).param("ipAddress", value)))
                    .andExpect(status().isBadRequest())
                    .andExpect(view().name("admin/ip-blocks"))
                    .andExpect(model().attributeExists("error"));
        }
        verify(blocks, never()).block(anyString(), anyLong());
    }

    @Test
    void onlyAdministratorsWithCsrfCanBlockAnEmail() throws Exception {
        String email = "blocked@example.test";
        var admin = user(member(Role.ADMIN));
        mvc.perform(withActualCsrf(post(BLOCK_EMAIL).param("email", email)))
                .andExpect(status().isUnauthorized());
        mvc.perform(withActualCsrf(post(BLOCK_EMAIL).with(user(member(Role.USER))).param("email", email)))
                .andExpect(status().isForbidden());
        mvc.perform(post(BLOCK_EMAIL).with(admin).param("email", email))
                .andExpect(status().isForbidden());
        verify(emailVerificationService, never()).blockEmail(anyString());

        mvc.perform(withActualCsrf(post(BLOCK_EMAIL).with(admin).param("email", email)))
                .andExpect(redirectedUrl(PAGE + "#blocked-email-title"));
        verify(emailVerificationService).blockEmail(email);
    }

    @Test
    void invalidEmailReturnsTheBlockPageWithAnEmailError() throws Exception {
        String message = "이메일을 확인해 주세요";
        doThrow(new IllegalArgumentException(message)).when(emailVerificationService).blockEmail("invalid");

        mvc.perform(withActualCsrf(post(BLOCK_EMAIL).with(user(member(Role.ADMIN))).param("email", "invalid")))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("admin/ip-blocks"))
                .andExpect(model().attribute("emailError", message));
    }

    @Test
    void administratorPageShowsIpAndEmailBlocksWithTheirTimestamps() throws Exception {
        String email = "blocked@example.test";
        Instant blockedAt = Instant.parse("2026-11-26T02:03:04Z");
        var emailBlock = mock(EmailVerificationAttempt.class);
        when(emailBlock.getBlockedEmail()).thenReturn(email);
        when(emailBlock.getBlockedAt()).thenReturn(blockedAt);
        var legacyBlock = mock(EmailVerificationAttempt.class);
        when(legacyBlock.getBlockedAt()).thenReturn(blockedAt.minusSeconds(60));
        when(emailVerificationService.listBlockedEmails()).thenReturn(List.of(emailBlock, legacyBlock));
        when(blocks.list()).thenReturn(List.of(new BlockedIp("203.0.113.12", 7L)));

        mvc.perform(get(PAGE).with(user(member(Role.USER)))).andExpect(status().isForbidden());
        verifyNoInteractions(emailVerificationService);

        String html = mvc.perform(get(PAGE).with(user(member(Role.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andReturn().getResponse().getContentAsString();
        String blockedTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault()).format(blockedAt);
        assertThat(html).contains("IP·이메일 영구 차단", "Blocked IP List", "Blocked Email List",
                "action=\"/admin/ip-blocks/email\"", "203.0.113.12", email, blockedTime, "주소 미기록");
    }

    private MockHttpServletRequestBuilder withActualCsrf(MockHttpServletRequestBuilder request) throws Exception {
        var page = mvc.perform(get(PAGE).with(user(member(Role.ADMIN))))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("ADMIN-IP-BLOCK-CSRF"))
                .andReturn().getResponse();
        var token = Pattern.compile("name=\"_csrf\"\\s+value=\"([^\"]+)\"").matcher(page.getContentAsString());
        assertThat(token.find()).isTrue();
        return request.cookie(page.getCookie("ADMIN-IP-BLOCK-CSRF")).param("_csrf", token.group(1));
    }

    private Member member(Role role) {
        return Member.builder().id(7L).email("member@example.test").pwd("unused").role(role).build();
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({IpBlockSecurityConfig.class, IpBlockController.class, IpBlockFacade.class})
    static class Config {
        @Bean
        IpBlockService blocks() { return mock(IpBlockService.class); }

        @Bean
        EmailVerificationService emailVerificationService() { return mock(EmailVerificationService.class); }

        @Bean
        IpBlockClientAddress clientAddress() { return new IpBlockClientAddress(List.of()); }

        @Bean
        JwtAuthenticationFilter jwtFilter() {
            return new JwtAuthenticationFilter(mock(JwtTokenProvider.class), mock(MemberService.class),
                    mock(JwtCookieService.class), mock(RefreshTokenService.class), mock(MemberTokenCacheService.class),
                    mock(OnlineUserService.class), mock(RefreshTokenRotation.class));
        }

        @Bean
        ThymeleafViewResolver viewResolver() {
            var templates = new ClassLoaderTemplateResolver();
            templates.setPrefix("templates/");
            templates.setSuffix(".html");
            templates.setCharacterEncoding("UTF-8");

            var engine = new SpringTemplateEngine();
            engine.setTemplateResolver(templates);
            var views = new ThymeleafViewResolver();
            views.setTemplateEngine(engine);
            views.setCharacterEncoding("UTF-8");
            return views;
        }
    }
}
