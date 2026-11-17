package spring.study.common.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import spring.study.member.entity.Member;

/**
 * Provides the authenticated member from the JWT-populated SecurityContext.
 */
@Service
public class JwtManager {
    public Member getLoginMember(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Member authenticatedMember)) return null;

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
