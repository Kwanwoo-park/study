package spring.study.common.component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import spring.study.member.entity.Member;
import spring.study.member.service.MemberService;

@RequiredArgsConstructor
@Component
public class SessionValidationInterceptor implements HandlerInterceptor {
    private final MemberService memberService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("member") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.sendRedirect("/member/login?error=true&exception=Session Expired");
            return false;
        }

        Member member = (Member) session.getAttribute("member");

        if (!memberService.validateSession(request)) {
            session.invalidate();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.sendRedirect("/member/login?error=true&exception=Session Invalid");
            return false;
        }

        if (member == null) {
            session.invalidate();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.sendRedirect("/member/login?error=true&exception=Not Found");
            return false;
        } else if (member.checkProfile()) {
            response.sendRedirect("redirect:/member/updatePhone");
            return false;
        }

        return true;
    }
}
