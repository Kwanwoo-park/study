package spring.study.admin.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import spring.study.admin.config.IntegrationEventLogSecurityConfig;
import spring.study.admin.dto.IntegrationEventLogResponse;
import spring.study.admin.service.IntegrationEventLogQueryService;
import spring.study.common.service.OnlineUserService;
import spring.study.jwt.component.*;
import spring.study.jwt.service.*;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;

import java.util.List;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static spring.study.admin.entity.IntegrationEventLog.*;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(IntegrationEventLogSecurityTest.Config.class)
@WebAppConfiguration
class IntegrationEventLogSecurityTest {
    @Autowired WebApplicationContext context;
    @Autowired IntegrationEventLogQueryService service;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        reset(service); mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    private Member member(Role role) { return Member.builder().id(7L).email("test@example.test").pwd("unused").role(role).build(); }

    @Test
    void directApiRequiresAdminRegardlessOfReferer() throws Exception {
        mvc.perform(get("/api/admin/event-logs").header("Referer", "/admin/administrator")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/event-logs").with(user(member(Role.USER)))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/event-logs/12").header("Referer", "/admin/event-logs")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/event-logs/12").with(user(member(Role.USER)))).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test
    void pageIsNotFoundForNonAdmins() throws Exception {
        mvc.perform(get("/admin/event-logs")).andExpect(status().isNotFound());
        mvc.perform(get("/admin/event-logs").with(user(member(Role.USER)))).andExpect(status().isNotFound());
        verifyNoInteractions(service);
    }
    @Test
    void adminCanReadPageAndApiWithoutCaching() throws Exception {
        when(service.find(null, null, null, null, 24)).thenReturn(new IntegrationEventLogResponse(List.of(), null, 7, null));
        mvc.perform(get("/admin/event-logs").with(user(member(Role.ADMIN))))
                .andExpect(status().isOk()).andExpect(view().name("admin/event_logs")).andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get("/api/admin/event-logs").with(user(member(Role.ADMIN))))
                .andExpect(status().isOk()).andExpect(jsonPath("entries").isArray()).andExpect(header().string("Cache-Control", "no-store"));
    }
    @Test
    void invalidEnumNeverReachesQueryService() throws Exception {
        mvc.perform(get("/api/admin/event-logs?broker=bad").with(user(member(Role.ADMIN)))).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void adminCanReadDetailWithoutCachingOrPayload() throws Exception {
        when(service.findById(12L)).thenReturn(new IntegrationEventLogResponse.Item(12L, LocalDateTime.now(), "node-1",
                Broker.KAFKA, "topic", Route.CHAT, Operation.PUBLISH, Outcome.FAILED, 55L, 1, 2, null, "java.lang.IllegalStateException"));
        mvc.perform(get("/api/admin/event-logs/12").with(user(member(Role.ADMIN))))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("id").value(12)).andExpect(jsonPath("referenceId").value(55))
                .andExpect(jsonPath("errorType").value("java.lang.IllegalStateException"))
                .andExpect(jsonPath("payload").doesNotExist()).andExpect(jsonPath("stackTrace").doesNotExist());
    }

    @Test
    void missingDetailPreservesNotFoundStatusAndInvalidIdIsRejected() throws Exception {
        when(service.findById(12L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "기록이 없습니다."));
        mvc.perform(get("/api/admin/event-logs/12").with(user(member(Role.ADMIN))))
                .andExpect(status().isNotFound()).andExpect(jsonPath("message").value("기록이 없습니다."))
                .andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get("/api/admin/event-logs/not-a-number").with(user(member(Role.ADMIN)))).andExpect(status().isBadRequest());
        verify(service).findById(12L); verifyNoMoreInteractions(service);
    }

    @Configuration
    @EnableWebMvc @EnableWebSecurity @EnableMethodSecurity
    @Import({IntegrationEventLogSecurityConfig.class, IntegrationEventLogController.class})
    static class Config {
        @Bean IntegrationEventLogQueryService service() { return mock(IntegrationEventLogQueryService.class); }
        @Bean JwtAuthenticationFilter jwtFilter() {
            return new JwtAuthenticationFilter(mock(JwtTokenProvider.class), mock(JwtCookieService.class), mock(RefreshTokenService.class), mock(MemberTokenCacheService.class), mock(OnlineUserService.class));
        }
        @Bean InternalResourceViewResolver viewResolver() { return new InternalResourceViewResolver("/templates/", ".html"); }
    }
}
