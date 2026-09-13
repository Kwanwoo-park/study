package spring.study.admin.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.client.RestTemplate;
import spring.study.jwt.component.JwtAuthenticationFilter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.HttpURLConnection;

@Configuration
public class GitHubHistoryConfig {
    @Bean
    public RestTemplate gitHubHistoryRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String method) throws IOException {
                super.prepareConnection(connection, method);
                // Never forward a server-side GitHub token to a redirected host.
                connection.setInstanceFollowRedirects(false);
            }
        };
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        return new RestTemplate(factory);
    }

    @Bean
    @Order(2)
    @ConditionalOnWebApplication
    public SecurityFilterChain gitHubHistoryFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        return http
                .securityMatcher("/admin/github", "/admin/github/**", "/api/admin/github", "/api/admin/github/**")
                .authorizeHttpRequests(authorize -> authorize.anyRequest().hasRole("ADMIN"))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, error) -> deny(request, response, 401))
                        .accessDeniedHandler((request, response, error) -> deny(request, response, 403)))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    private static void deny(HttpServletRequest request, HttpServletResponse response, int status) throws IOException {
        response.setHeader("Cache-Control", "no-store");
        if (request.getServletPath().startsWith("/api/") || request.getRequestURI().startsWith(request.getContextPath() + "/api/")) {
            response.setStatus(status);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"관리자 인증이 필요합니다\"}");
        } else {
            response.sendError(404);
        }
    }
}
