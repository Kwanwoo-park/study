package spring.study.admin.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import spring.study.jwt.component.JwtAuthenticationFilter;

@Configuration @ConditionalOnWebApplication
public class KafkaOperationsSecurityConfig {
    @Bean @Order(4)
    public SecurityFilterChain kafkaOperationsFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        CookieCsrfTokenRepository csrf = new CookieCsrfTokenRepository();
        csrf.setCookieName("KAFKA-ADMIN-CSRF"); csrf.setCookiePath("/api/admin/kafka");
        return http.securityMatcher("/admin/kafka", "/admin/kafka/**", "/api/admin/kafka", "/api/admin/kafka/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("ADMIN"))
                .csrf(config -> config.csrfTokenRepository(csrf))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, error) -> response.sendError(request.getRequestURI().contains("/api/") ? 401 : 404))
                        .accessDeniedHandler((request, response, error) -> response.sendError(request.getRequestURI().contains("/api/") ? 403 : 404)))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class).build();
    }
}
