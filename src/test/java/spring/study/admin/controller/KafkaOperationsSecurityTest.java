package spring.study.admin.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import spring.study.admin.config.KafkaOperationsSecurityConfig;
import spring.study.common.service.OnlineUserService;
import spring.study.jwt.component.*;
import spring.study.jwt.service.*;
import spring.study.kafka.service.*;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(KafkaOperationsSecurityTest.Config.class) @WebAppConfiguration
class KafkaOperationsSecurityTest {
    @Autowired WebApplicationContext context;
    @Autowired KafkaMonitoringService monitoring;
    @Autowired KafkaDeadLetterService deadLetters;
    MockMvc mvc;
    @BeforeEach void setup() { reset(monitoring, deadLetters); mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build(); }
    private Member member(Role role) { return Member.builder().id(5L).email("test@example.test").pwd("unused").role(role).build(); }
    @Test void readAndReplayRequireAdministrator() throws Exception {
        mvc.perform(get("/admin/kafka")).andExpect(status().isNotFound());
        mvc.perform(get("/api/admin/kafka/overview")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/kafka/dead-letters").with(user(member(Role.USER)))).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/kafka/dead-letters/1/replay").with(user(member(Role.USER))).with(csrf())).andExpect(status().isForbidden());
        verifyNoInteractions(monitoring, deadLetters);
    }
    @Test void adminReplayRequiresCsrfAndIsAcceptedAsynchronously() throws Exception {
        mvc.perform(post("/api/admin/kafka/dead-letters/1/replay").with(user(member(Role.ADMIN)))).andExpect(status().isForbidden());
        verifyNoInteractions(deadLetters);
        mvc.perform(post("/api/admin/kafka/dead-letters/1/replay").with(user(member(Role.ADMIN))).with(csrf()))
                .andExpect(status().isAccepted()).andExpect(header().string("Cache-Control", "no-store"));
        verify(deadLetters).requestReplay(1L, 5L);
    }
    @Test void adminCanReadPageAndList() throws Exception {
        when(deadLetters.list(null, null)).thenReturn(new KafkaDeadLetterService.Listing(List.of(), null));
        mvc.perform(get("/admin/kafka").with(user(member(Role.ADMIN)))).andExpect(status().isOk()).andExpect(view().name("admin/kafka_operations"));
        mvc.perform(get("/api/admin/kafka/dead-letters").with(user(member(Role.ADMIN)))).andExpect(status().isOk()).andExpect(jsonPath("entries").isArray());
    }
    @Configuration @EnableWebMvc @EnableWebSecurity @EnableMethodSecurity
    @Import({KafkaOperationsSecurityConfig.class, KafkaOperationsController.class})
    static class Config {
        @Bean KafkaMonitoringService monitoring() { return mock(KafkaMonitoringService.class); }
        @Bean KafkaDeadLetterService deadLetters() { return mock(KafkaDeadLetterService.class); }
        @Bean JwtAuthenticationFilter jwtFilter() { return new JwtAuthenticationFilter(mock(JwtTokenProvider.class), mock(JwtCookieService.class), mock(RefreshTokenService.class), mock(MemberTokenCacheService.class), mock(OnlineUserService.class)); }
        @Bean InternalResourceViewResolver views() { return new InternalResourceViewResolver("/templates/", ".html"); }
    }
}
