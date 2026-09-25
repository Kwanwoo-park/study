package spring.study.admin.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import spring.study.jwt.component.JwtAuthenticationFilter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
@ConditionalOnWebApplication
public class IntegrationEventLogSecurityConfig {
    @Bean
    @Order(3)
    public SecurityFilterChain integrationEventLogFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        return http.securityMatcher("/admin/event-logs", "/admin/event-logs/**", "/api/admin/event-logs", "/api/admin/event-logs/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("ADMIN"))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, error) -> deny(request, response, 401))
                        .accessDeniedHandler((request, response, error) -> deny(request, response, 403)))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class).build();
    }

    private static void deny(HttpServletRequest request, HttpServletResponse response, int status) throws IOException {
        response.setHeader("Cache-Control", "no-store");
        if (request.getRequestURI().startsWith(request.getContextPath() + "/api/")) {
            response.setStatus(status); response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"message\":\"관리자 인증이 필요합니다\"}");
        } else response.sendError(404);
    }
}
