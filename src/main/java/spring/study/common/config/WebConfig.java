package spring.study.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import spring.study.common.auth.LoginMemberArgumentResolver;
import spring.study.common.component.SessionValidationInterceptor;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final SessionValidationInterceptor sessionValidationInterceptor;
    private final LoginMemberArgumentResolver loginMemberArgumentResolver;

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

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(loginMemberArgumentResolver);
    }
}
