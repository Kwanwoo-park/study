package spring.study.common.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import spring.study.member.entity.Member;

@Service
public class SessionManager {
    public Member getLoginMember(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (Member) session.getAttribute("member");
    }

    public HttpSession getSession(HttpServletRequest request) {
        return request.getSession(false);
    }

    public void setLoginMember(HttpServletRequest request, Member member) {
        HttpSession session = request.getSession();

        session.setAttribute("IP", getIp(request));
        session.setAttribute("UA", request.getHeader("User-Agent"));
        session.setAttribute("member", member);
    }

    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        session.removeAttribute("member");
        session.removeAttribute("IP");
        session.removeAttribute("UA");
    }

    private String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip))
            ip = request.getHeader("Proxy-Client-IP");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip))
            ip = request.getHeader("WL-Proxy-Client-IP");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip))
            ip = request.getHeader("HTTP_CLIENT-IP");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip))
            ip = request.getHeader("HTTP_X-FORWARDED_FOR");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip))
            ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip))
            ip = request.getHeader("X-RealIP");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip))
            ip = request.getHeader("REMOTE_ADDR");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip))
            ip = request.getRemoteAddr();

        return ip.split(",")[0];
    }
}
