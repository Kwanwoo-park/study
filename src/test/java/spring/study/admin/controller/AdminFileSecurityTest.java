package spring.study.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.AopTestUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import spring.study.admin.config.AdminFileSecurityConfig;
import spring.study.admin.dto.AdminFileResponseDto;
import spring.study.admin.dto.Download;
import spring.study.admin.entity.AdminFile;
import spring.study.admin.facade.AdminFileFacade;
import spring.study.admin.service.AdminFileService;
import spring.study.common.service.JwtManager;
import spring.study.common.service.OnlineUserService;
import spring.study.jwt.component.JwtAuthenticationFilter;
import spring.study.jwt.component.JwtTokenProvider;
import spring.study.jwt.component.RefreshTokenRotation;
import spring.study.jwt.service.JwtCookieService;
import spring.study.jwt.service.MemberTokenCacheService;
import spring.study.jwt.service.RefreshTokenService;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.service.MemberService;
import spring.study.forbidden.service.ForbiddenService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import com.fasterxml.jackson.databind.JsonNode;
import org.mockito.InOrder;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringJUnitConfig(AdminFileSecurityTest.Config.class)
@WebAppConfiguration
class AdminFileSecurityTest {
    private static final String ID = "d0b3808b-6be3-44c1-80cf-ef639895ed9d";
    @Autowired WebApplicationContext context;
    @Autowired AdminFileService securedService;
    AdminFileService service;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = AopTestUtils.getUltimateTargetObject(securedService);
        reset(service);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void uploadsRequireAdministratorAndCsrf() throws Exception {
        mvc.perform(withActualCsrf(multipart("/api/admin/files").file(executable()))).andExpect(status().isUnauthorized());
        mvc.perform(withActualCsrf(multipart("/api/admin/files").file(executable()).with(user(member(Role.USER))))).andExpect(status().isForbidden());
        mvc.perform(multipart("/api/admin/files").file(executable()).with(user(member(Role.ADMIN)))).andExpect(status().isForbidden());
        mvc.perform(multipart("/api/admin/files").file(executable()).with(user(member(Role.ADMIN))).header("X-XSRF-TOKEN", "invalid")).andExpect(status().isForbidden());
        verify(service, never()).upload(any(), any());
    }

    @Test
    void downloadIsAnAttachmentAndNeverInlineExecutableContent() throws Exception {
        byte[] bytes = {77, 90, 0, (byte) 255};
        when(service.download(ID)).thenReturn(new Download(metadata(), new ByteArrayResource(bytes)));
        mvc.perform(get("/api/admin/files/" + ID + "/download").with(user(member(Role.ADMIN))))
                .andExpect(status().isOk()).andExpect(content().bytes(bytes))
                .andExpect(content().contentType("application/octet-stream"))
                .andExpect(header().string("Content-Disposition", containsString("attachment;")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Content-Security-Policy", containsString("sandbox")));
    }

    private Member member(Role role) {
        return Member.builder().id(7L).email("admin@example.test").pwd("unused").role(role).build();
    }

    private MockHttpServletRequestBuilder withActualCsrf(MockHttpServletRequestBuilder request) throws Exception {
        MockHttpServletResponse response = mvc.perform(get("/api/admin/files/csrf").with(user(member(Role.ADMIN)))).andExpect(status().isOk()).andReturn().getResponse();
        JsonNode token = new ObjectMapper().readTree(response.getContentAsString());
        return request.cookie(response.getCookie("ADMIN-FILES-CSRF")).header(token.get("headerName").asText(), token.get("token").asText());
    }

    private MockMultipartFile executable() {
        return new MockMultipartFile("file", "setup.exe", "application/octet-stream", new byte[]{77, 90, 0, (byte) 255});
    }

    private AdminFileResponseDto metadata() {
        return AdminFileResponseDto.from(new AdminFile(ID, "setup.exe", 4, 7L));
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @EnableMethodSecurity
    @Import({AdminFileSecurityConfig.class, AdminFileController.class, AdminFileFacade.class, AdminViewController.class, spring.study.admin.facade.AdminViewFacade.class})
    static class Config {
        @Bean
        MemberService memberService() { return mock(MemberService.class); }

        @Bean
        JwtManager jwtManager() { return mock(JwtManager.class); }

        @Bean
        ForbiddenService forbiddenService() { return mock(ForbiddenService.class); }

        @Bean
        AdminFileService service() { return mock(AdminFileService.class, withSettings().withoutAnnotations()); }

        @Bean
        MultipartProperties multipartProperties() {
            MultipartProperties properties = new MultipartProperties();
            properties.setMaxFileSize(DataSize.ofMegabytes(20));
            properties.setMaxRequestSize(DataSize.ofMegabytes(210));
            return properties;
        }

        @Bean
        JwtAuthenticationFilter jwtFilter() {
            return new JwtAuthenticationFilter(mock(JwtTokenProvider.class), mock(MemberService.class), mock(JwtCookieService.class), mock(RefreshTokenService.class), mock(MemberTokenCacheService.class), mock(OnlineUserService.class), mock(RefreshTokenRotation.class));
        }

        @Bean
        InternalResourceViewResolver viewResolver() { return new InternalResourceViewResolver("/templates/", ".html"); }
    }
}
