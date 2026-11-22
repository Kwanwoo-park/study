package spring.study.common.facade;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import spring.study.common.component.ModerationPolicy;
import spring.study.jwt.service.JwtCookieService;
import spring.study.member.entity.Member;

@Service
@RequiredArgsConstructor
public class ModerationFacade {
    private final ModerationPolicy policy;
    private final JwtCookieService jwtCookieService;

    public int validate(String content, Member member, HttpServletResponse response) {
        int risk = policy.validate(content, member);
        if (risk == 3) {
            jwtCookieService.clearAuthenticationCookies(response);
            SecurityContextHolder.clearContext();
        }
        return risk;
    }
}
