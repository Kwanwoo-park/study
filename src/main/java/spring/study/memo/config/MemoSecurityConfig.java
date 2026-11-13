package spring.study.memo.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import spring.study.jwt.component.JwtAuthenticationFilter;

@Configuration
@ConditionalOnWebApplication
public class MemoSecurityConfig {
    @Bean
    @Order(5)
    public SecurityFilterChain memoFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        CookieCsrfTokenRepository csrf = new CookieCsrfTokenRepository();
        csrf.setCookieName("MEMOS-CSRF");
        csrf.setCookiePath("/api");
        return http.securityMatcher("/memos", "/memos/**", "/api/memos", "/api/memos/**",
                        "/admin/memos", "/admin/memos/**", "/api/admin/memos", "/api/admin/memos/**")
                .authorizeHttpRequests(authorize -> authorize.anyRequest().hasAnyRole("USER", "ADMIN"))
                .csrf(config -> config.csrfTokenRepository(csrf))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, error) -> response.sendError(request.getRequestURI().contains("/api/") ? 401 : 404))
                        .accessDeniedHandler((request, response, error) -> response.sendError(request.getRequestURI().contains("/api/") ? 403 : 404)))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(RequestCacheConfigurer::disable)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class).build();
    }
}
