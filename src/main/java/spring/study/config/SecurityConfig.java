package spring.study.config;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import spring.study.component.CustomAuthenticationProvider;
import spring.study.component.AuthFailureHandler;
import spring.study.component.AuthSuccessHandler;
import spring.study.service.member.MemberService;
import spring.study.service.oauth.CustomOAuth2UserService;

@RequiredArgsConstructor
@EnableWebSecurity
@EnableMethodSecurity
@Configuration
public class SecurityConfig{
    private final MemberService memberService;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final AuthSuccessHandler authSuccessHandler;
    private final AuthFailureHandler authFailureHandler;
    @Bean
    public BCryptPasswordEncoder encryptPassword() {
        return new BCryptPasswordEncoder();
    }
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .authorizeHttpRequests()
                .requestMatchers("/**", "/login/**", "/register/**", "/detail/**", "/find/**", "/updatePassword/**").permitAll()
                .requestMatchers("/js/**", "/css/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .and()
                .oauth2Login()
                .defaultSuccessUrl("/board/main", true)
                .userInfoEndpoint()
                .userService(customOAuth2UserService)
                .and()
                .loginPage("/member/login")
                .successHandler(authSuccessHandler)
                .failureHandler(authFailureHandler)
                .defaultSuccessUrl("/board/main")
                .and()
                .logout()
                .logoutRequestMatcher(new AntPathRequestMatcher("/member/logout"))
                .logoutSuccessUrl("/member/login")
                .invalidateHttpSession(true)
                .deleteCookies("member").permitAll()
                .and()
                .sessionManagement()
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
                .expiredUrl("/login?error-true&exception=Hava been attempted to login form a new place or session expired");

        return http.build();
    }
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity httpSecurity, BCryptPasswordEncoder bCryptPasswordEncoder) throws Exception{
        AuthenticationManagerBuilder authenticationManagerBuilder = httpSecurity.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.authenticationProvider(new CustomAuthenticationProvider(memberService, bCryptPasswordEncoder));
        return authenticationManagerBuilder.build();
    }
}