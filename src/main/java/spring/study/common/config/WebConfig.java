package spring.study.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import spring.study.common.component.SessionValidationInterceptor;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final SessionValidationInterceptor sessionValidationInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(sessionValidationInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/member/login",
                        "/member/register",
                        "/api/member/login",
                        "/api/mail/confirm",
                        "/api/member/updatePassword",
                        "/api/member/updatePhone",
                        "/oauth/**",
                        "/css/**",
                        "/js/**",
                        "/img/**"
                );
    }
}
