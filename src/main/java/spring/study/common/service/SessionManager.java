package spring.study.common.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import spring.study.member.entity.Member;

/**
 * Compatibility facade for existing controllers. Authentication is now sourced
 * from the JWT-populated SecurityContext; no HTTP session is created or read.
 */
@Service
public class SessionManager {
    public Member getLoginMember(HttpServletRequest request) {
        Object principal = SecurityContextHolder.getContext().getAuthentication() == null
                ? null
                : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof Member authenticatedMember)) return null;

        if (authenticatedMember.isAccessBlocked()) {
            SecurityContextHolder.clearContext();
            return null;
        }
        return authenticatedMember;
    }

    public void logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
    }
}
