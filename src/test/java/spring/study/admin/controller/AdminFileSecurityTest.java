package spring.study.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import spring.study.admin.config.AdminFileSecurityConfig;
import spring.study.admin.dto.AdminFileResponseDto;
import spring.study.admin.entity.AdminFile;
import spring.study.admin.service.AdminFileService;
import spring.study.common.service.OnlineUserService;
import spring.study.jwt.component.JwtAuthenticationFilter;
import spring.study.jwt.component.JwtTokenProvider;
import spring.study.jwt.service.JwtCookieService;
import spring.study.jwt.service.MemberTokenCacheService;
import spring.study.jwt.service.RefreshTokenService;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

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

    @ParameterizedTest
    @ValueSource(strings = {"/admin/files", "/api/admin/files", "/api/admin/files/csrf", "/api/admin/files/d0b3808b-6be3-44c1-80cf-ef639895ed9d/download"})
    void anonymousCannotAccessFiles(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/admin/files", "/api/admin/files", "/api/admin/files/csrf", "/api/admin/files/d0b3808b-6be3-44c1-80cf-ef639895ed9d/download"})
    void normalMemberCannotAccessFiles(String path) throws Exception {
        mvc.perform(get(path).with(user(member(Role.USER)))).andExpect(status().isForbidden());
        verifyNoInteractions(service);
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
    void administratorCanUploadUsingActualCsrfCookieAndResponseHeaderToken() throws Exception {
        when(service.maxFileSize()).thenReturn(10485760L);
        when(service.upload(any(), eq(7L))).thenReturn(metadata());
        var tokenResponse = mvc.perform(get("/api/admin/files/csrf").with(user(member(Role.ADMIN))))
                .andExpect(status().isOk()).andExpect(jsonPath("maxFileSize").value(10485760)).andReturn().getResponse();
        var token = new ObjectMapper().readTree(tokenResponse.getContentAsString());
        mvc.perform(multipart("/api/admin/files").file(executable()).with(user(member(Role.ADMIN)))
                        .cookie(tokenResponse.getCookie("ADMIN-FILES-CSRF"))
                        .header(token.get("headerName").asText(), token.get("token").asText()))
                .andExpect(status().isCreated()).andExpect(jsonPath("originalFilename").value("setup.exe"))
                .andExpect(jsonPath("s3Bucket").doesNotExist()).andExpect(jsonPath("s3Key").doesNotExist()).andExpect(jsonPath("s3VersionId").doesNotExist());
        verify(service).upload(any(), eq(7L));
    }

    @Test
    void administratorCanViewPageAndList() throws Exception {
        when(service.list(0)).thenReturn(Page.empty());
        mvc.perform(get("/admin/files").with(user(member(Role.ADMIN)))).andExpect(status().isOk()).andExpect(view().name("admin/files"));
        mvc.perform(get("/api/admin/files").with(user(member(Role.ADMIN))))
                .andExpect(status().isOk()).andExpect(jsonPath("content").isArray()).andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void downloadIsAnAttachmentAndNeverInlineExecutableContent() throws Exception {
        byte[] bytes = {77, 90, 0, (byte) 255};
        when(service.download(ID)).thenReturn(new AdminFileService.Download(metadata(), new ByteArrayResource(bytes)));
        mvc.perform(get("/api/admin/files/" + ID + "/download").with(user(member(Role.ADMIN))))
                .andExpect(status().isOk()).andExpect(content().bytes(bytes))
                .andExpect(content().contentType("application/octet-stream"))
                .andExpect(header().string("Content-Disposition", containsString("attachment;")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Content-Security-Policy", containsString("sandbox")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "HEAD", "RANGE"})
    void closesS3ConnectionAfterAttachmentResponse(String method) throws Exception {
        AtomicBoolean closed = new AtomicBoolean();
        ByteArrayInputStream source = new ByteArrayInputStream(new byte[]{77, 90, 0, (byte) 255}) {
            @Override
            public void close() throws IOException {
                closed.set(true);
                super.close();
            }
        };
        InputStreamResource resource = new InputStreamResource(new S3ObjectInputStream(source, null));
        when(service.download(ID)).thenReturn(new AdminFileService.Download(metadata(), resource));
        var downloadRequest = request(method.equals("RANGE") ? HttpMethod.GET : HttpMethod.valueOf(method), "/api/admin/files/" + ID + "/download").with(user(member(Role.ADMIN)));
        if (method.equals("RANGE")) downloadRequest.header("Range", "bytes=0-1");
        mvc.perform(downloadRequest).andExpect(status().isOk()).andExpect(header().string("Content-Length", "4"));
        assertThat(closed).isTrue();
    }

    private Member member(Role role) {
        return Member.builder().id(7L).email("admin@example.test").pwd("unused").role(role).build();
    }

    private MockHttpServletRequestBuilder withActualCsrf(MockHttpServletRequestBuilder request) throws Exception {
        var response = mvc.perform(get("/api/admin/files/csrf").with(user(member(Role.ADMIN)))).andExpect(status().isOk()).andReturn().getResponse();
        var token = new ObjectMapper().readTree(response.getContentAsString());
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
    @Import({AdminFileSecurityConfig.class, AdminFileController.class})
    static class Config {
        @Bean
        AdminFileService service() { return mock(AdminFileService.class, withSettings().withoutAnnotations()); }

        @Bean
        JwtAuthenticationFilter jwtFilter() {
            return new JwtAuthenticationFilter(mock(JwtTokenProvider.class), mock(JwtCookieService.class), mock(RefreshTokenService.class), mock(MemberTokenCacheService.class), mock(OnlineUserService.class));
        }

        @Bean
        InternalResourceViewResolver viewResolver() { return new InternalResourceViewResolver("/templates/", ".html"); }
    }
}
