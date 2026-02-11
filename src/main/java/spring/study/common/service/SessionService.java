package spring.study.common.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import spring.study.member.entity.Member;

@Service
public class SessionService {
    public Member getLoginMember(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (Member) session.getAttribute("member");
    }

    public void setLoginMember(HttpServletRequest request, String ip, Member member) {
        HttpSession session = request.getSession();

        session.setAttribute("IP", ip);
        session.setAttribute("UA", request.getHeader("User-Agent"));
        session.setAttribute("member", member);
    }

    public void logout(HttpServletRequest request, String ip) {
        HttpSession session = request.getSession(false);

        session.removeAttribute("member");
        session.removeAttribute("IP");
        session.removeAttribute("UA");
    }
}
