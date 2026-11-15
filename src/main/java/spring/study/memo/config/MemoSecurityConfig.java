package spring.study.memo.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;
import spring.study.jwt.component.JwtAuthenticationFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
@ConditionalOnWebApplication
public class MemoSecurityConfig {
    @Bean
    @Order(5)
    public SecurityFilterChain memoFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        CookieCsrfTokenRepository csrf = new CookieCsrfTokenRepository();

        csrf.setCookieName("MEMOS-CSRF");
        csrf.setCookiePath("/api");

        return http.securityMatcher("/memos", "/memos/**", "/api/memos", "/api/memos/**")
                .authorizeHttpRequests(authorize -> authorize.anyRequest().hasAnyRole("USER", "ADMIN"))
                // JWT restores authentication on every request; retain CSRF verification without login-time token removal.
                .csrf(config -> config.csrfTokenRepository(csrf).sessionAuthenticationStrategy(new NullAuthenticatedSessionStrategy()))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, error) -> response.sendError(request.getRequestURI().contains("/api/") ? 401 : 404))
                        .accessDeniedHandler(MemoSecurityConfig::handleAccessDenied))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(RequestCacheConfigurer::disable)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class).build();
    }

    private static void handleAccessDenied(HttpServletRequest request, HttpServletResponse response, AccessDeniedException error) throws IOException {
        if (!request.getRequestURI().contains("/api/")) {
            response.sendError(404);
            return;
        }

        boolean csrfFailure = error instanceof CsrfException;
        String code = csrfFailure ? "CSRF_INVALID" : "FORBIDDEN";
        String message = csrfFailure ? "요청 인증 정보가 변경되었습니다. 다시 시도해 주세요." : "메모를 사용할 권한이 없습니다.";

        response.setStatus(403);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }
}
