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
}
