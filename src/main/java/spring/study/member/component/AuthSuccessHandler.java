package spring.study.member.component;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import spring.study.member.entity.Member;
import spring.study.jwt.service.JwtAuthenticationService;
import spring.study.member.service.MemberService;

import java.io.IOException;

@RequiredArgsConstructor
@Component
public class AuthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final MemberService memberService;
    private final JwtAuthenticationService jwtAuthenticationService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        String email = authentication.getPrincipal() instanceof OAuth2User oauth2User
                ? oauth2User.getAttribute("email")
                : authentication.getName();
        Member member = memberService.findMember(email);
        jwtAuthenticationService.login(member, response);
        setDefaultTargetUrl("/board/main");
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
